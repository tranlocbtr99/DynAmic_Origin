package vn.loctm.originbridge;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

/** Read-only event observation. No notification cancellation or forwarding until OEM verification. */
public final class BridgeListener extends NotificationListenerService {
  @Override public void onNotificationPosted(StatusBarNotification sbn) {
    if (sbn != null && !getPackageName().equals(sbn.getPackageName()))
      Log.d("LTMOriginBridge","Notification received from " + sbn.getPackageName());
  }
}
