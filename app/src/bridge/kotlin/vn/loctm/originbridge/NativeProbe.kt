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

    private fun payload(context: Context, revision: Int): Bundle {
        val title = "LTM Origin Bridge"
        val content = "Kiểm chứng lần $revision"
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
                .setExtras(payload(context, revision))
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
