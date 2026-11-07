package vn.loctm.originbridge

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Bundle

/** Isolated manual probes. Never schedules repeated notifications or modifies SystemUI. */
object ProtocolMatrix {
    data class Case(val id: String, val scene: String, val rightTemplate: Int, val forceShow: Boolean = false, val permanent: Boolean = false)
    val cases = listOf(
        Case("BASE_TRAIN", "TRAIN", 6),
        Case("TEXT_TRAIN", "TRAIN", 4),
        Case("PROGRESS_TRAIN", "TRAIN", 2),
        Case("FORCE_TRAIN", "TRAIN", 6, forceShow = true),
        Case("PERMANENT_TRAIN", "TRAIN", 6, permanent = true)
    )
    private const val CHANNEL = "ltm_protocol_matrix"
    private const val ID = 61062
    fun post(context: Context, test: Case): String {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return "NO_MANAGER"
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "OriginOS protocol matrix", NotificationManager.IMPORTANCE_LOW))
        Diagnostics.record(context, NativeProbe.sceneRegistration(context))
        val icon = Icon.createWithResource(context, android.R.drawable.ic_dialog_info)
        val title = "LTM Origin Bridge"
        val content = "Protocol: ${test.id}"
        val island = Bundle().apply {
            putInt("island.superx.leftTemplate", 1)
            putInt("island.superx.rightTemplate", test.rightTemplate)
            if (test.forceShow) putBoolean("island.superx.forceShow", true)
            if (test.permanent) putBoolean("island.superx.permanent", true)
            putBundle("island.superx.leftInfo", Bundle().apply {
                putString("island.superx.leftInfo.content", title)
                putParcelable("island.superx.leftInfo.icon", icon)
            })
            putBundle("island.superx.rightInfo", Bundle().apply {
                when (test.rightTemplate) {
                    2 -> {
                        putInt("island.superx.rightInfo.progressValue", 40)
                        putInt("island.superx.rightInfo.progressState", 0)
                        putInt("island.superx.rightInfo.progressColor", 0xff80cbc4.toInt())
                    }
                    4 -> {
                        putString("island.superx.rightInfo.content", "Test")
                        putParcelable("island.superx.rightInfo.icon", icon)
                    }
                    else -> {
                        putString("island.superx.rightInfo.capsuleContent", "Test")
                        putInt("island.superx.rightInfo.capsuleBgColor", 0xff80cbc4.toInt())
                    }
                }
            })
        }
        val extras = Bundle().apply {
            putInt("notification.superx.operation", 0)
            putBoolean("notification.superx.showNotify", true)
            putInt("notification.superx.template", if (test.rightTemplate == 2) 2 else 1)
            putString("notification.superx.scene", test.scene)
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
                if (test.rightTemplate == 2) {
                    putInt("notification.superx.infos.progress", 40)
                    putInt("notification.superx.infos.progressColor", 0xff80cbc4.toInt())
                    putParcelableArrayList("notification.superx.infos.nodeIcon", arrayListOf(icon, icon))
                    putParcelable("notification.superx.infos.indicatorIcon", icon)
                    putInt("notification.superx.infos.indicatorLoc", 1)
                } else {
                    putString("notification.superx.infos.describe", title)
                    putString("notification.superx.infos.coreInfo", content)
                    putParcelable("notification.superx.infos.image", icon)
                }
            })
            putBundle("notification.superx.shortInfos", Bundle().apply {
                putString("notification.superx.shortInfos.describeShort", title)
                putString("notification.superx.shortInfos.coreInfoShort", content)
                putParcelable("notification.superx.shortInfos.image", icon)
            })
            putBundle("notification.superx.island", island)
        }
        return try {
            manager.notify(ID, Notification.Builder(context, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title).setContentText(content)
                .setExtras(extras).setOnlyAlertOnce(true).setOngoing(true).build())
            Diagnostics.record(context, "MATRIX_POST_${test.id}")
            "POST_REQUESTED"
        } catch (e: Exception) {
            Diagnostics.record(context, "MATRIX_ERROR_${test.id}_${e.javaClass.simpleName}")
            "ERROR_${e.javaClass.simpleName}"
        }
    }
    fun clear(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        try {
            manager.notify(ID, Notification.Builder(context, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setExtras(Bundle().apply {
                    putInt("notification.superx.operation", 2)
                    putString("notification.superx.scene", "TRAIN")
                    putBoolean("notification.superx.showNotify", false)
                }).build())
        } catch (_: Exception) { }
        manager.cancel(ID)
        Diagnostics.record(context, "MATRIX_CLEARED")
    }
}
