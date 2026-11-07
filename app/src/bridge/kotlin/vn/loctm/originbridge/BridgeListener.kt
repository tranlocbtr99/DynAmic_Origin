package vn.loctm.originbridge

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class BridgeListener : NotificationListenerService() {
    private var activeSource: String? = null
    private var registered = false

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!enabled(this) || sbn.packageName == packageName) return
        val source = sbn.notification
        if ((source.flags and Notification.FLAG_GROUP_SUMMARY) != 0 ||
            source.visibility == Notification.VISIBILITY_SECRET ||
            getSystemService(KeyguardManager::class.java)?.isKeyguardLocked != false
        ) return

        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (!manager.areNotificationsEnabled()) return
        if (!registered) {
            val result = NativeProbe.sceneRegistration(this)
            Diagnostics.record(this, result)
            if (result != "SCENE_REGISTERED") return
            registered = true
        }

        val name = try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(sbn.packageName, 0))
                .toString().take(50)
        } catch (_: android.content.pm.PackageManager.NameNotFoundException) {
            "Ứng dụng"
        }
        val content = "Có thông báo mới"
        try {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, "Thẻ SuperX", NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { setSound(null, null); enableVibration(false) }
            )
            val card = Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(name)
                .setContentText(content)
                .setContentIntent(source.contentIntent)
                .setOnlyAlertOnce(true)
                .setVisibility(Notification.VISIBILITY_SECRET)
                .setTimeoutAfter(8_000)
                .setExtras(NativeProbe.payload(this, name, content).apply {
                    putBoolean("notification.superx.showNotify", false)
                })
                .build()
            manager.notify(TAG, CARD_ID, card)
            activeSource = sbn.key
        } catch (e: RuntimeException) {
            Diagnostics.record(this, "FORWARD_ERROR_" + e.javaClass.simpleName)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.key == activeSource) {
            activeSource = null
            clearCard(this)
        }
    }

    override fun onListenerDisconnected() {
        registered = false
        activeSource = null
        if (packageName == NATIVE_PACKAGE) clearCard(this)
    }

    companion object {
        const val NATIVE_PACKAGE = "com.autonavi.minimap"
        private const val CHANNEL = "dynamic_superx_forward"
        private const val TAG = "VIVO_SUPERX_TAG"
        private const val CARD_ID = 61062

        fun enabled(context: Context): Boolean =
            context.packageName == NATIVE_PACKAGE &&
                context.getSharedPreferences("superx", Context.MODE_PRIVATE).getBoolean("forward", false)

        fun setEnabled(context: Context, enabled: Boolean) {
            context.getSharedPreferences("superx", Context.MODE_PRIVATE).edit()
                .putBoolean("forward", enabled).apply()
            if (!enabled) clearCard(context)
        }

        private fun clearCard(context: Context) {
            context.getSystemService(NotificationManager::class.java)?.cancel(TAG, CARD_ID)
        }
    }
}
