package vn.loctm.dynamicorigin;

import android.content.*;
import android.provider.Settings;

public class RecoveryReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context c, Intent i) {
    if (i == null
        || (!Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())
            && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(i.getAction()))
        || !Prefs.enabled(c)) return;
    android.service.notification.NotificationListenerService.requestRebind(
        new ComponentName(c, NotificationBridge.class));
    if (Settings.canDrawOverlays(c))
      try {
        c.startForegroundService(new Intent(c, IslandService.class));
      } catch (RuntimeException ex) {
        Prefs.get(c)
            .edit()
            .putString("error", "Mở ứng dụng để bật lại đảo: " + ex.getClass().getSimpleName())
            .apply();
      }
  }
}
