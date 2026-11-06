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
    if (Prefs.enabled(this)) {
      StatusBarNotification[] a = getActiveNotifications();
      if (a != null) for (StatusBarNotification s : a) capture(s, false);
    }
  }

  @Override
  public void onListenerDisconnected() {
    reference.clear();
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

  private void capture(StatusBarNotification s, boolean alert) {
    if (!Prefs.enabled(this)
        || s.getPackageName().equals(getPackageName())
        || !Prefs.allowed(this, s.getPackageName())) return;
    Notification n = s.getNotification();
    if ((n.flags & Notification.FLAG_GROUP_SUMMARY) != 0) return;
    Entry e = new Entry();
    e.key = s.getKey();
    e.pkg = s.getPackageName();
    e.time = s.getPostTime();
    e.progress = n.extras.getInt(Notification.EXTRA_PROGRESS, 0);
    e.progressMax = n.extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0);
    e.indeterminate = n.extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false);
    e.title = String.valueOf(n.extras.getCharSequence(Notification.EXTRA_TITLE, ""));
    e.text =
        String.valueOf(
            n.extras.getCharSequence(
                Notification.EXTRA_BIG_TEXT,
                n.extras.getCharSequence(Notification.EXTRA_TEXT, "")));
    if (e.title.isBlank() && e.text.isBlank()) return;
    e.title = e.title.substring(0, Math.min(300, e.title.length()));
    e.text = e.text.substring(0, Math.min(4000, e.text.length()));
    try {
      e.app =
          getPackageManager()
              .getApplicationLabel(getPackageManager().getApplicationInfo(e.pkg, 0))
              .toString();
      e.icon = getPackageManager().getApplicationIcon(e.pkg);
    } catch (Exception ex) {
      e.app = e.pkg;
    }
    e.open = n.contentIntent;
    e.actions = n.actions;
    e.media =
        n.extras.getParcelable(
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
    IslandService service = IslandService.getInstance();
    if (service != null) service.refresh(expand);
    if (TransferPolicy.allowed(
        saved,
        alert,
        service != null && service.ready(),
        Prefs.get(this).getBoolean("transfer", false),
        e.ongoing)) {
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
