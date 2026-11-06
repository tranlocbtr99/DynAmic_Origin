package vn.loctm.dynamicorigin;

import android.content.*;
import java.util.*;
import org.json.*;

final class Inbox {
  private static final LinkedHashMap<String, Entry> entries = new LinkedHashMap<>();
  private static boolean loaded;

  static synchronized void load(Context c) {
    if (loaded) return;
    loaded = true;
    try {
      JSONArray list = new JSONArray(Prefs.get(c).getString("inbox", "[]"));
      for (int i = 0; i < list.length(); i++) {
        JSONObject o = list.getJSONObject(i);
        Entry e = new Entry();
        e.key = o.getString("key");
        e.pkg = o.getString("pkg");
        e.app = o.getString("app");
        e.title = o.getString("title");
        e.text = o.getString("text");
        e.time = o.getLong("time");
        e.removed = true;
        entries.put(e.key, e);
      }
    } catch (JSONException ignored) {
    }
  }

  static synchronized boolean put(Context c, Entry e) {
    load(c);
    Entry old = entries.get(e.key);
    if (old != null) e.transferred = old.transferred;
    entries.remove(e.key);
    entries.put(e.key, e);
    while (entries.size() > 80) entries.remove(entries.keySet().iterator().next());
    return save(c);
  }

  static synchronized boolean save(Context c) {
    JSONArray a = new JSONArray();
    try {
      for (Entry e : entries.values()) {
        JSONObject o = new JSONObject();
        o.put("key", e.key);
        o.put("pkg", e.pkg);
        o.put("app", e.app);
        o.put("title", e.title);
        o.put("text", e.text);
        o.put("time", e.time);
        a.put(o);
      }
    } catch (JSONException ex) {
      return false;
    }
    return Prefs.get(c).edit().putString("inbox", a.toString()).commit();
  }

  static synchronized java.util.List<Entry> list(Context c) {
    load(c);
    ArrayList<Entry> a = new ArrayList<>(entries.values());
    Collections.reverse(a);
    return a;
  }

  static synchronized void remove(Context c, String key) {
    entries.remove(key);
    save(c);
  }

  static synchronized void clear(Context c) {
    entries.clear();
    save(c);
  }

  static synchronized void sourceRemoved(Context c, String key) {
    load(c);
    Entry e = entries.get(key);
    if (e != null) {
      e.removed = true;
      e.actions = null;
      e.media = null;
      if (!e.transferred) e.open = null;
    }
  }
}
