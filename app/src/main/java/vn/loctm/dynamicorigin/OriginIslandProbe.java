package vn.loctm.dynamicorigin;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;

/**
 * Experimental OriginOS 6 native-island protocol probe.
 *
 * This class is deliberately NOT connected to the existing overlay service.
 * Calling showProbe() is an explicit opt-in diagnostic action only.
 * OEM private extras are undocumented and may be ignored by the firmware.
 */
public final class OriginIslandProbe {
    private static final String CHANNEL_ID = "ltm_origin_bridge_probe";
    private static final int NOTIFICATION_ID = 61061;
    private OriginIslandProbe() {}

    public static void showProbe(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;
        manager.createNotificationChannel(new NotificationChannel(
                CHANNEL_ID, "Origin Island protocol test",
                NotificationManager.IMPORTANCE_LOW));

        Bundle extras = new Bundle();
        extras.putInt("notification.superx.operation", 0);
        extras.putBoolean("notification.superx.showNotify", true);
        // The capsule/island/template schema is OEM-private and must be
        // verified on-device before enabling any richer payloads.
        Notification notification = new Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("LTM Origin Bridge — thử nghiệm")
                .setContentText("Kiểm tra Origin Island gốc; có thể chỉ hiện trong khay thông báo.")
                .setExtras(extras)
                .setAutoCancel(false)
                .build();
        manager.notify(NOTIFICATION_ID, notification);
    }

    public static void clearProbe(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) manager.cancel(NOTIFICATION_ID);
    }
}
