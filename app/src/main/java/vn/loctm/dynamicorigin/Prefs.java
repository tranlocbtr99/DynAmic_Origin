package vn.loctm.dynamicorigin;

import android.content.*;

final class Prefs {
  static android.content.SharedPreferences get(Context c) {
    return c.getSharedPreferences("island", 0);
  }

  static boolean enabled(Context c) {
    return get(c).getBoolean("enabled", false);
  }

  static int number(Context c, String key, int fallback) {
    return get(c).getInt(key, fallback);
  }

  static boolean allowed(Context c, String pkg) {
    return !get(c).getStringSet("excluded_apps", java.util.Collections.emptySet()).contains(pkg);
  }

  static void allow(Context c, String pkg, boolean on) {
    java.util.Set<String> excluded =
        new java.util.HashSet<>(
            get(c).getStringSet("excluded_apps", java.util.Collections.emptySet()));
    if (on) excluded.remove(pkg);
    else excluded.add(pkg);
    get(c).edit().putStringSet("excluded_apps", excluded).apply();
  }
}
