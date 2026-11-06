package vn.loctm.dynamicorigin;

import android.app.*;
import android.content.*;
import android.service.notification.*;

public class NotificationBridge extends NotificationListenerService {
  private static java.lang.ref.WeakReference<NotificationBridge> reference =
      new java.lang.ref.WeakReference<>(null);

  static NotificationBridge getInstance() {
    return reference.get();
  }

  @Override
  public void onListenerConnected() {
    reference = new java.lang.ref.WeakReference<>(this);
    Prefs.get(this).edit().putString("listener_status", "Đã kết nối").apply();
    if (Prefs.enabled(this)) sync(this);
  }

  @Override
  public void onListenerDisconnected() {
    reference.clear();
    Prefs.get(this).edit().putString("listener_status", "Mất kết nối").apply();
    if (Prefs.enabled(this)) requestRebind(new ComponentName(this, NotificationBridge.class));
  }

  @Override
  public void onDestroy() {
    if (getInstance() == this) reference.clear();
    super.onDestroy();
  }

  @Override
  public void onNotificationPosted(StatusBarNotification s) {
    capture(s, true);
  }

  void replay() {
    StatusBarNotification[] a = getActiveNotifications();
    if (a != null) for (StatusBarNotification s : a) capture(s, false);
  }

  static void sync(Context context) {
    if (!context
        .getSystemService(NotificationManager.class)
        .isNotificationListenerAccessGranted(
            new ComponentName(context, NotificationBridge.class))) {
      Prefs.get(context)
          .edit()
          .putString("listener_status", "Chưa cấp quyền Truy cập thông báo")
          .apply();
      return;
    }

    NotificationBridge bridge = getInstance();
    if (bridge != null) {
      try {
        bridge.replay();
      } catch (RuntimeException ex) {
        Prefs.get(context)
            .edit()
            .putString("listener_status", "Đồng bộ lỗi: " + ex.getClass().getSimpleName())
            .apply();
      }
    } else {
      requestRebind(new ComponentName(context, NotificationBridge.class));
      Prefs.get(context).edit().putString("listener_status", "Đã yêu cầu kết nối lại").apply();
    }
  }

  private void capture(StatusBarNotification s, boolean alert) {
    if (s == null) return;
    try {
      captureInternal(s, alert);
    } catch (RuntimeException ex) {
      Prefs.get(this)
          .edit()
          .putString("listener_status", "Lỗi đọc thông báo: " + ex.getClass().getSimpleName())
          .apply();
    }
  }

  private static String plain(CharSequence value) {
    return value == null ? "" : value.toString();
  }

  private void captureInternal(StatusBarNotification s, boolean alert) {
    if (!Prefs.enabled(this)
        || s.getPackageName().equals(getPackageName())
        || !Prefs.allowed(this, s.getPackageName())) return;
    Notification n = s.getNotification();
    android.os.Bundle extras = n.extras == null ? android.os.Bundle.EMPTY : n.extras;
    Prefs.get(this).edit().putLong("last_notification_seen", System.currentTimeMillis()).apply();
    if ((n.flags & Notification.FLAG_GROUP_SUMMARY) != 0) {
      StatusBarNotification[] active = getActiveNotifications();
      if (active != null)
        for (StatusBarNotification child : active)
          if (child.getPackageName().equals(s.getPackageName())
              && java.util.Objects.equals(child.getGroupKey(), s.getGroupKey())
              && (child.getNotification().flags & Notification.FLAG_GROUP_SUMMARY) == 0) return;
    }

    Entry e = new Entry();
    e.key = s.getKey();
    e.pkg = s.getPackageName();
    e.time = s.getPostTime();
    e.progress = extras.getInt(Notification.EXTRA_PROGRESS, 0);
    e.progressMax = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0);
    e.indeterminate = extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false);
    e.title = plain(extras.getCharSequence(Notification.EXTRA_TITLE));
    e.text = plain(extras.getCharSequence(Notification.EXTRA_BIG_TEXT));
    if (e.text.isBlank()) e.text = plain(extras.getCharSequence(Notification.EXTRA_TEXT));
    if (e.text.isBlank()) {
      CharSequence[] lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
      if (lines != null) e.text = android.text.TextUtils.join("\n", lines);
    }
    if (e.text.isBlank()) {
      android.os.Parcelable[] messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES);
      if (messages != null)
        for (int i = messages.length - 1; i >= 0; i--)
          if (messages[i] instanceof android.os.Bundle) {
            String text = plain(((android.os.Bundle) messages[i]).getCharSequence("text"));
            if (!text.isBlank()) {
              e.text = text;
              break;
            }
          }
    }
    try {
      e.app =
          getPackageManager()
              .getApplicationLabel(getPackageManager().getApplicationInfo(e.pkg, 0))
              .toString();
      e.icon = getPackageManager().getApplicationIcon(e.pkg);
    } catch (Exception ex) {
      e.app = e.pkg;
    }
    if (e.title.isBlank()) e.title = e.app;
    if (e.text.isBlank()) e.text = plain(extras.getCharSequence(Notification.EXTRA_SUB_TEXT));
    if (e.text.isBlank()) e.text = "Thông báo mới từ " + e.app;
    e.title = e.title.substring(0, Math.min(300, e.title.length()));
    e.text = e.text.substring(0, Math.min(4000, e.text.length()));
    e.open = n.contentIntent;
    e.actions = n.actions;
    e.media =
        extras.getParcelable(
            Notification.EXTRA_MEDIA_SESSION, android.media.session.MediaSession.Token.class);
    e.ongoing =
        (n.flags
                    & (Notification.FLAG_ONGOING_EVENT
                        | Notification.FLAG_FOREGROUND_SERVICE
                        | Notification.FLAG_NO_CLEAR))
                != 0
            || Notification.CATEGORY_CALL.equals(n.category)
            || Notification.CATEGORY_ALARM.equals(n.category)
            || e.progressMax > 0
            || e.indeterminate
            || e.media != null
            || e.pkg.equals("android")
            || e.pkg.equals("com.android.systemui");
    Entry previous = Inbox.find(this, e.key);
    boolean expand =
        alert
            && UpdatePolicy.shouldExpand(
                previous != null,
                e.ongoing,
                e.title,
                e.text,
                previous == null ? null : previous.title,
                previous == null ? null : previous.text);
    boolean saved = Inbox.put(this, e);
    if (saved)
      Prefs.get(this)
          .edit()
          .putLong("last_notification_saved", System.currentTimeMillis())
          .putString("last_notification_app", e.app)
          .apply();
    IslandService service = IslandService.getInstance();
    if (service != null) service.refresh(expand);
    if (TransferPolicy.allowed(
        saved,
        alert,
        service != null && service.ready(),
        Prefs.get(this).getBoolean("transfer", false),
        e.ongoing,
        getSystemService(KeyguardManager.class).isKeyguardLocked())) {
      e.transferred = true;
      try {
        cancelNotification(e.key);
      } catch (RuntimeException ex) {
        e.transferred = false;
      }
    }
  }

  @Override
  public void onNotificationRemoved(StatusBarNotification s) {
    Inbox.sourceRemoved(this, s.getKey());
    if (IslandService.getInstance() != null) IslandService.getInstance().refresh(false);
  }

  static void dismiss(String key) {
    if (getInstance() != null)
      try {
        getInstance().cancelNotification(key);
      } catch (RuntimeException ignored) {
      }
  }
}
