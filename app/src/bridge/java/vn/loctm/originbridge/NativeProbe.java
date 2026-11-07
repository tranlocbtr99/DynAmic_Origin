package vn.loctm.originbridge;

import android.app.*;
import android.content.Context;
import android.os.Bundle;

public final class NativeProbe {
  private static final String CHANNEL = "ltm_bridge_diagnostic";
  private static final int ID = 61061;
  private NativeProbe() {}
  public static void show(Context context) {
    NotificationManager manager = context.getSystemService(NotificationManager.class);
    if (manager == null) return;
    manager.createNotificationChannel(new NotificationChannel(CHANNEL,"Origin Island diagnostics",NotificationManager.IMPORTANCE_DEFAULT));
    Bundle extras = new Bundle();
    // Unofficial OEM protocol hints, not proof of OriginOS support.
    extras.putInt("notification.superx.operation",0);
    extras.putBoolean("notification.superx.showNotify",true);
    Notification notification = new Notification.Builder(context,CHANNEL)
      .setSmallIcon(android.R.drawable.ic_dialog_info)
      .setContentTitle("LTM Origin Bridge · Diagnostic")
      .setContentText("Kiểm tra Origin Island gốc hoặc khay thông báo")
      .setExtras(extras).setAutoCancel(false).build();
    manager.notify(ID,notification);
  }
  public static void clear(Context context) {
    NotificationManager manager = context.getSystemService(NotificationManager.class);
    if (manager != null) manager.cancel(ID);
  }
}
