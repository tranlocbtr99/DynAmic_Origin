package com.originisle.android.ui;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONObject;

public final class LtmUpdateJob extends JobService {
    private static final int JOB = 20101, NOTICE = 20102;
    private static final String CHANNEL = "ltmori_release_updates";
    private static final String OPEN = "com.originisle.android.VIEW_RELEASE_NOTES";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static long lastForegroundCheck = -1;
    private AtomicBoolean cancelled;

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences("ltmori_release_updates", MODE_PRIVATE);
    }

    public static void onForeground(Activity activity) {
        JobScheduler scheduler = activity.getSystemService(JobScheduler.class);
        if (scheduler != null && scheduler.getPendingJob(JOB) == null) {
            try {
                scheduler.schedule(new JobInfo.Builder(JOB, new ComponentName(activity, LtmUpdateJob.class))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPeriodic(TimeUnit.HOURS.toMillis(12)).setPersisted(true).build());
            } catch (IllegalArgumentException | IllegalStateException | SecurityException ignored) {
                // Foreground checks still work if this device rejects the scheduled job.
            }
        }
        Intent intent = activity.getIntent();
        if (intent != null && OPEN.equals(intent.getAction())) {
            intent.setAction(null);
            LtmUpgrade.open(activity);
            return;
        }
        long now = SystemClock.elapsedRealtime();
        if (lastForegroundCheck >= 0 && now - lastForegroundCheck < TimeUnit.MINUTES.toMillis(15)) return;
        lastForegroundCheck = now;
        LtmUpgrade.enqueue(() -> {
            try {
                JSONObject release = LtmUpgrade.newRelease(activity);
                if (release == null) return;
                String tag = release.getString("tag_name");
                MAIN.post(() -> {
                    if (activity.isFinishing() || activity.isDestroyed() || !activity.hasWindowFocus()) return;
                    if (tag.equals(prefs(activity).getString("seen", ""))) return;
                    LtmUpgrade.showRelease(activity, release);
                });
            } catch (Exception ignored) {
                // The manual check still reports network errors.
            }
        });
    }

    static void markSeen(Context context, String tag) {
        prefs(context).edit().putString("seen", tag).apply();
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) manager.cancel(NOTICE);
    }

    @Override public boolean onStartJob(JobParameters params) {
        AtomicBoolean stopped = new AtomicBoolean();
        cancelled = stopped;
        LtmUpgrade.enqueue(() -> {
            try {
                if (!stopped.get()) {
                    JSONObject release = LtmUpgrade.newRelease(this);
                    if (release != null && !stopped.get()) postRelease(release);
                }
            } catch (Exception ignored) {
                // The next periodic run retries the check.
            } finally {
                MAIN.post(() -> {
                    if (!stopped.get()) jobFinished(params, false);
                });
            }
        });
        return true;
    }

    @Override public boolean onStopJob(JobParameters params) {
        if (cancelled != null) cancelled.set(true);
        return false;
    }

    private void postRelease(JSONObject release) throws Exception {
        String tag = release.getString("tag_name");
        SharedPreferences settings = prefs(this);
        if (tag.equals(settings.getString("notified", "")) || tag.equals(settings.getString("seen", ""))) return;
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager == null || !manager.areNotificationsEnabled()) return;
        manager.createNotificationChannel(new NotificationChannel(CHANNEL, "Cập nhật LTMori", NotificationManager.IMPORTANCE_DEFAULT));
        if (manager.getNotificationChannel(CHANNEL).getImportance() == NotificationManager.IMPORTANCE_NONE) return;
        Intent intent = new Intent(OPEN).setClassName(this, "com.originisle.android.MainActivity")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent open = PendingIntent.getActivity(this, NOTICE, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        String notes = LtmUpgrade.notes(release);
        String summary = notes.length() > 180 ? notes.substring(0, 180).strip() + "…" : notes;
        manager.notify(NOTICE, new Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("LTMori " + tag + " đã có")
                .setContentText(summary)
                .setStyle(new Notification.BigTextStyle().bigText(summary))
                .setContentIntent(open).setAutoCancel(true).build());
        settings.edit().putString("notified", tag).apply();
    }
}
