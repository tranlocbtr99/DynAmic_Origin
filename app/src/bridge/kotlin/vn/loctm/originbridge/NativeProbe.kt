package vn.loctm.originbridge

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Bundle

/**
 * OriginOS 6 protocol experiment based on publicly documented wire keys.
 * See docs/ORIGIN_PROTOCOL.md for provenance and verification limitations.
 */
object NativeProbe {
    private const val CHANNEL = "ltm_bridge_diagnostic_high_v2"
    private const val ID = 61061
    private const val SCENE = "TRAIN"

    private fun manager(context: Context): NotificationManager? =
        context.getSystemService(NotificationManager::class.java)

    fun sceneRegistration(context: Context): String = try {
        val method = NotificationManager::class.java.getMethod(
            "setSuperXInfosSceneList",
            MutableList::class.java, MutableList::class.java,
            MutableList::class.java, MutableList::class.java
        )
        method.invoke(
            manager(context),
            arrayListOf(SCENE), arrayListOf("true"),
            arrayListOf(context.packageName), arrayListOf("true")
        )
        "SCENE_REGISTERED"
    } catch (e: java.lang.reflect.InvocationTargetException) {
        "SCENE_UNAVAILABLE_" + (e.targetException?.javaClass?.simpleName ?: "UNKNOWN")
    } catch (e: Exception) {
        "SCENE_UNAVAILABLE_" + e.javaClass.simpleName
    }

    /** Read-only OEM probes: distinguish missing method, access restriction and scene denial. */
    fun environmentReport(context: Context): String {
        val nm = manager(context) ?: return "NO_NOTIFICATION_MANAGER"
        val lines = mutableListOf(
            "BRAND=" + android.os.Build.BRAND,
            "MANUFACTURER=" + android.os.Build.MANUFACTURER,
            "MODEL=" + android.os.Build.MODEL,
            "SDK=" + android.os.Build.VERSION.SDK_INT,
            "NOTIFICATIONS_ENABLED=" + nm.areNotificationsEnabled(),
            "CHANNEL_IMPORTANCE=" + (nm.getNotificationChannel(CHANNEL)?.importance?.toString() ?: "NOT_CREATED")
        )
        fun query(methodName: String): String = try {
            val method = NotificationManager::class.java.getMethod(
                methodName, String::class.java, String::class.java
            )
            val result = method.invoke(nm, context.packageName, SCENE)
            result?.toString() ?: "NULL"
        } catch (e: java.lang.reflect.InvocationTargetException) {
            "INVOCATION_" + (e.targetException?.javaClass?.simpleName ?: "UNKNOWN")
        } catch (e: NoSuchMethodException) {
            "METHOD_MISSING"
        } catch (e: SecurityException) {
            "ACCESS_DENIED"
        } catch (e: Exception) {
            "UNAVAILABLE_" + e.javaClass.simpleName
        }
        lines.add("SCENE_STATUS_BEFORE=" + query("getSceneStatus"))
        lines.add("SCENE_REGISTER_ACTION=" + sceneRegistration(context))
        lines.add("SCENE_STATUS_AFTER=" + query("getSceneStatus"))
        lines.add("CUSTOM_FUNCTION=" + query("isSupportCustomFun"))
        return lines.joinToString("\n")
    }

    internal fun payload(context: Context, title: String, content: String): Bundle {
        val icon = Icon.createWithResource(context, android.R.drawable.ic_dialog_info)
        return Bundle().apply {
            putInt("notification.superx.operation", 0)
            putBoolean("notification.superx.showNotify", true)
            putInt("notification.superx.template", 1)
            putString("notification.superx.scene", SCENE)
            putInt("notification.superx.changedRecord", 0)
            putBundle("notification.superx.baseInfos", Bundle().apply {
                putCharSequence("notification.superx.baseInfos.title", title)
                putCharSequence("notification.superx.baseInfos.content", content)
                putParcelable("notification.superx.baseInfos.icon", icon)
            })
            putBundle("notification.superx.capsule", Bundle().apply {
                putInt("notification.superx.capsule.state", 1)
                putCharSequence("notification.superx.capsule.content", title)
                putParcelable("notification.superx.capsule.icon", icon)
            })
            putBundle("notification.superx.infos", Bundle().apply {
                putString("notification.superx.infos.describe", title)
                putString("notification.superx.infos.coreInfo", content)
                putParcelable("notification.superx.infos.image", icon)
            })
            putBundle("notification.superx.shortInfos", Bundle().apply {
                putString("notification.superx.shortInfos.describeShort", title)
                putString("notification.superx.shortInfos.coreInfoShort", content)
                putParcelable("notification.superx.shortInfos.image", icon)
            })
            putBundle("notification.superx.island", Bundle().apply {
                putInt("island.superx.leftTemplate", 1)
                putInt("island.superx.rightTemplate", 6)
                putBundle("island.superx.leftInfo", Bundle().apply {
                    putString("island.superx.leftInfo.content", title)
                    putParcelable("island.superx.leftInfo.icon", icon)
                })
                putBundle("island.superx.rightInfo", Bundle().apply {
                    putString("island.superx.rightInfo.capsuleContent", content)
                    putInt("island.superx.rightInfo.capsuleBgColor", 0xff80cbc4.toInt())
                })
            })
        }
    }

    fun send(context: Context, revision: Int) {
        val manager = manager(context) ?: run {
            Diagnostics.record(context, "ERROR_NO_NOTIFICATION_MANAGER")
            return
        }
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "Origin Island diagnostics", NotificationManager.IMPORTANCE_HIGH)
        )
        Diagnostics.record(context, sceneRegistration(context))
        try {
            val notification = Notification.Builder(context, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("LTM Origin Bridge")
                .setContentText("Kiểm chứng lần $revision")
                .setExtras(payload(context, "LTM Origin Bridge", "Kiểm chứng lần $revision"))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .build()
            manager.notify("VIVO_SUPERX_TAG", ID, notification)
            Diagnostics.record(context, if (revision == 1) "SEND_REQUESTED_V2" else "UPDATE_REQUESTED_V2")
        } catch (e: Exception) {
            Diagnostics.record(context, "POST_ERROR_" + e.javaClass.simpleName)
        }
    }

    fun clear(context: Context) {
        val manager = manager(context) ?: return
        try {
            val end = Notification.Builder(context, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setExtras(Bundle().apply { putInt("notification.superx.operation", 2) })
                .build()
            manager.notify("VIVO_SUPERX_TAG", ID, end)
            Diagnostics.record(context, "UNMOUNT_REQUESTED_V2")
        } catch (e: Exception) {
            Diagnostics.record(context, "UNMOUNT_ERROR_" + e.javaClass.simpleName)
        } finally {
            manager.cancel("VIVO_SUPERX_TAG", ID)
            Diagnostics.record(context, "CANCEL_REQUESTED_V2")
        }
    }
}

/** Live card protocol shared by samples and incoming notifications. */
internal object SuperX {
    const val TAG = "VIVO_SUPERX_TAG"
    const val CHANNEL = "dynamic_superx_live_high"
    private const val SCENE = "NAVIGATION"
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val pendingEnds = mutableMapOf<Int, Runnable>()
    private const val N = "notification.superx"
    private const val I = "island.superx"
    private val scenes = listOf(
        SCENE, "MOVIE", "HEALTH_REGISTER", "TAXI", "TAKEOUT", "DELIEVERY",
        "CAR_STATE", "METTING", "TRAIN", "FLIGHT", "INCALLING", "VOIPCALL",
        "TIMER", "RIDE_GUIDE", "CRITICAL"
    )

    data class Card(
        val title: String,
        val text: String,
        val app: String,
        val icon: Icon,
        val click: android.app.PendingIntent?,
        val ongoing: Boolean = false,
        val progress: Int? = null,
        val actions: List<Notification.Action> = emptyList()
    )

    private fun register(manager: NotificationManager): String? = try {
        val list = List::class.java
        NotificationManager::class.java.getMethod("setSuperXInfosSceneList", list, list, list, list)
            .invoke(manager, ArrayList(scenes), ArrayList(scenes.map { "true" }),
                ArrayList(scenes.map { BridgeListener.NATIVE_PACKAGE }), ArrayList(scenes.map { "true" }))
        null
    } catch (error: Exception) {
        val cause = (error as? java.lang.reflect.InvocationTargetException)?.targetException ?: error
        cause.javaClass.simpleName
    }

    private fun channelManager(context: Context): NotificationManager? {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return null
        if (!manager.areNotificationsEnabled()) return null
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "Origin Island", NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            }
        )
        return manager
    }

    private fun payload(card: Card, revision: Int, lockscreen: Boolean) = Bundle().apply {
        val chip = card.progress?.let { "$it%" } ?: card.text.take(24)
        putInt("$N.operation", 0)
        putBoolean("$N.showNotify", true)
        putInt("$N.template", 4)
        putString("$N.scene", SCENE)
        putInt("$N.changedRecord", revision)
        putBoolean("$N.sound", false)
        putBoolean("$N.dismissWhenKill", true)
        putBoolean("$N.disableInvertColor", true)
        putInt("$N.displays", 256 or 65536 or (if (lockscreen) 16 else 0))
        card.click?.let { putParcelable("$N.clickResp", it) }
        putBundle("$N.baseInfos", Bundle().apply {
            val key = "$N.baseInfos"
            val action = card.actions.firstOrNull()
            putParcelable("$key.icon", card.icon)
            putCharSequence("$key.title", card.title)
            putCharSequence("$key.content", card.text)
            putInt("$key.subInfo", if (action == null) 1 else 2)
            putString("$key.subText", action?.title?.toString() ?: card.app)
            action?.let {
                putParcelable("$key.subInfoClickResp", it.actionIntent)
                putInt("$key.subTextColor", 0xff1c1c1e.toInt())
                putInt("$key.subCapsuleBgColor", 0xfff2f2f2.toInt())
            }
        })
        putBundle("$N.infos", Bundle())
        putBundle("$N.shortInfos", Bundle().apply {
            val key = "$N.shortInfos"
            putParcelable("$key.image", card.icon)
            putString("$key.describeShort", card.title)
            putString("$key.coreInfoShort", card.text)
            card.click?.let { putParcelable("$key.imageClickResp", it) }
        })
        putBundle("$N.island", Bundle().apply {
            putInt("$I.leftTemplate", 1)
            putInt("$I.rightTemplate", if (card.progress == null) 4 else 2)
            putBoolean("$I.showBarWhenCard", false)
            putInt("$I.islandClick", 0)
            card.click?.let { putParcelable("$I.clickResp", it) }
            putBundle("$I.leftInfo", Bundle().apply {
                putParcelable("$I.leftInfo.icon", card.icon)
                putString("$I.leftInfo.content", "")
            })
            putBundle("$I.rightInfo", Bundle().apply {
                if (card.progress == null) {
                    putCharSequence("$I.rightInfo.content", chip)
                    card.click?.let { putParcelable("$I.rightInfo.clickResp", it) }
                } else {
                    putInt("$I.rightInfo.progressValue", card.progress)
                    putInt("$I.rightInfo.progressColor", 0xff34c759.toInt())
                    putInt("$I.rightInfo.progressState", 0)
                }
            })
        })
        putBundle("$N.capsule", Bundle().apply {
            val key = "$N.capsule"
            putInt("$key.state", 1)
            putParcelable("$key.icon", card.icon)
            putString("$key.content", chip)
            card.click?.let { putParcelable("$key.clickResp", it) }
        })
    }

    fun post(context: Context, id: Int, revision: Int, card: Card, lockscreen: Boolean): String {
        val manager = channelManager(context) ?: return "NOTIFICATIONS_DISABLED"
        val error = register(manager)
        if (error != null) return "SCENE_UNAVAILABLE_$error"
        if (manager.getNotificationChannel(CHANNEL).importance < NotificationManager.IMPORTANCE_HIGH) {
            return "CHANNEL_NOT_HIGH"
        }
        pendingEnds.remove(id)?.let(handler::removeCallbacks)
        manager.notify(TAG, id, Notification.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(card.title)
            .setContentText(card.text)
            .setSubText(card.app)
            .setOnlyAlertOnce(true)
            .setOngoing(card.ongoing)
            .setExtras(payload(card, revision, lockscreen))
            .build())
        return "SUPERX_POSTED"
    }

    fun end(context: Context, id: Int) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        try {
            if (channelManager(context) != null && register(manager) == null) {
                manager.notify(TAG, id, Notification.Builder(context, CHANNEL)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("")
                    .setContentText("")
                    .setExtras(Bundle().apply {
                        putInt("$N.operation", 2)
                        putString("$N.scene", SCENE)
                        putInt("$N.changedRecord", Int.MAX_VALUE)
                        putBoolean("$N.showNotify", false)
                    })
                    .build())
            }
        } finally {
            pendingEnds.remove(id)?.let(handler::removeCallbacks)
            val cancel = Runnable {
                pendingEnds.remove(id)
                manager.cancel(TAG, id)
            }
            pendingEnds[id] = cancel
            handler.postDelayed(cancel, 350L)
        }
    }
}
