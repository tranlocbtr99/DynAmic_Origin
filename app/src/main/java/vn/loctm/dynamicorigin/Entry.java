package vn.loctm.dynamicorigin;

import android.app.*;
import android.graphics.drawable.Drawable;

final class Entry {
  String key, pkg, app, title, text;
  long time;
  int progress, progressMax;
  boolean indeterminate;
  boolean ongoing, removed, transferred;
  PendingIntent open;
  Notification.Action[] actions;
  Drawable icon;
  android.media.session.MediaSession.Token media;
}
