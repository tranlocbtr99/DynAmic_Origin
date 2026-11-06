package vn.loctm.dynamicorigin;

import android.app.*;
import android.app.usage.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.drawable.Drawable;
import java.util.*;

final class RecentApps {
  static final class App {
    final String pkg, name;
    final Drawable icon;

    App(String p, String n, Drawable i) {
      pkg = p;
      name = n;
      icon = i;
    }
  }

  static boolean permitted(Context c) {
    return c.getSystemService(AppOpsManager.class)
            .unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), c.getPackageName())
        == AppOpsManager.MODE_ALLOWED;
  }

  static List<App> query(Context c) {
    if (!permitted(c)) return Collections.emptyList();
    PackageManager pm = c.getPackageManager();
    Set<String> launchable = new HashSet<>();
    for (ResolveInfo item :
        pm.queryIntentActivities(
            new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0))
      launchable.add(item.activityInfo.packageName);
    Set<String> excluded =
        new HashSet<>(Arrays.asList(c.getPackageName(), "android", "com.android.systemui"));
    for (ResolveInfo item :
        pm.queryIntentActivities(
            new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0))
      excluded.add(item.activityInfo.packageName);
    long now = System.currentTimeMillis();
    UsageEvents events =
        c.getSystemService(UsageStatsManager.class).queryEvents(now - 24 * 60 * 60 * 1000L, now);
    RecentOrder order = new RecentOrder();
    if (events == null) return Collections.emptyList();
    UsageEvents.Event event = new UsageEvents.Event();
    while (events.hasNextEvent()) {
      events.getNextEvent(event);
      String pkg = event.getPackageName();
      if (event.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED
          && launchable.contains(pkg)
          && !excluded.contains(pkg)) order.record(pkg, event.getTimeStamp());
    }
    List<App> result = new ArrayList<>();
    for (String pkg : order.newest(12))
      try {
        ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
        if (info.enabled)
          result.add(
              new App(pkg, pm.getApplicationLabel(info).toString(), pm.getApplicationIcon(info)));
      } catch (PackageManager.NameNotFoundException ignored) {
      }
    return result;
  }
}
