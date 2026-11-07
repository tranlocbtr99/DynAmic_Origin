package vn.loctm.originbridge

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/** No automatic forwarding until OEM protocol acceptance is verified on-device. */
class BridgeListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // Intentionally no logging of app identity, text, OTP or notification payload.
    }
}
