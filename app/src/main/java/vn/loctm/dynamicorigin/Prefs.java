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
}
