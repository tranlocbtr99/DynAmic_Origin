package vn.loctm.originbridge

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle

/** OEM extras are experimental. Notification delivery does not prove native-island rendering. */
object NativeProbe {
    private const val CHANNEL = "ltm_bridge_diagnostic"
    private const val ID = 61061
    fun send(context: Context, revision: Int) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Origin Island diagnostics", NotificationManager.IMPORTANCE_DEFAULT))
        val extras = Bundle().apply {
            putInt("notification.superx.operation", 0)
            putBoolean("notification.superx.showNotify", true)
        }
        val notification = Notification.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("LTM Origin Bridge · thử nghiệm")
            .setContentText("Bản cập nhật $revision · Kiểm tra Origin Island gốc")
            .setExtras(extras)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .build()
        manager.notify(ID, notification) // Same ID: updates existing Android notification.
        Diagnostics.record(context, if (revision == 1) "SEND_REQUESTED" else "UPDATE_REQUESTED")
    }
    fun clear(context: Context) {
        context.getSystemService(NotificationManager::class.java)?.cancel(ID)
        Diagnostics.record(context, "CANCEL_REQUESTED")
    }
}
