package vn.loctm.dynamicorigin;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.os.Bundle;
import android.provider.Settings;
import android.text.*;
import android.widget.*;
import java.util.*;

public class AppRulesActivity extends Activity {
  private final Map<String, String> apps = new TreeMap<>();
  private LinearLayout rows;

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    for (ResolveInfo info :
        getPackageManager()
            .queryIntentActivities(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)) {
      String pkg = info.activityInfo.packageName;
      if (!pkg.equals(getPackageName()))
        apps.put(pkg, info.loadLabel(getPackageManager()).toString());
    }
    for (Entry e : Inbox.all(this)) apps.put(e.pkg, e.app);
    LinearLayout body = new LinearLayout(this);
    body.setOrientation(LinearLayout.VERTICAL);
    int p = (int) (18 * getResources().getDisplayMetrics().density);
    body.setPadding(p, p, p, p);
    Button back = new Button(this);
    back.setText("‹ Cài đặt đảo");
    back.setOnClickListener(v -> finish());
    body.addView(back);
    TextView help = new TextView(this);
    help.setText(
        "Tắt ứng dụng để ngừng thu nhận và chuyển thông báo của ứng dụng đó. Bản hệ thống tiếp tục"
            + " hoạt động bình thường.");
    body.addView(help);
    EditText search = new EditText(this);
    search.setHint("Tìm ứng dụng");
    body.addView(search);
    ScrollView scroll = new ScrollView(this);
    rows = new LinearLayout(this);
    rows.setOrientation(LinearLayout.VERTICAL);
    scroll.addView(rows);
    body.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    setContentView(body);
    show("");
    search.addTextChangedListener(
        new TextWatcher() {
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          public void onTextChanged(CharSequence s, int start, int before, int count) {
            show(s.toString());
          }

          public void afterTextChanged(Editable e) {}
        });
  }

  private void show(String query) {
    rows.removeAllViews();
    String q = query.toLowerCase(Locale.ROOT);
    java.util.List<Map.Entry<String, String>> sorted = new ArrayList<>(apps.entrySet());
    sorted.sort(Map.Entry.comparingByValue(String.CASE_INSENSITIVE_ORDER));
    for (Map.Entry<String, String> app : sorted) {
      String pkg = app.getKey(), name = app.getValue();
      if (!name.toLowerCase(Locale.ROOT).contains(q) && !pkg.contains(q)) continue;
      LinearLayout row = new LinearLayout(this);
      row.setOrientation(LinearLayout.VERTICAL);
      Switch toggle = new Switch(this);
      toggle.setText(name);
      toggle.setChecked(Prefs.allowed(this, pkg));
      toggle.setOnCheckedChangeListener(
          (v, on) -> {
            Prefs.allow(this, pkg, on);
            NotificationBridge bridge = NotificationBridge.getInstance();
            if (on && bridge != null) bridge.replay();
            IslandService service = IslandService.getInstance();
            if (service != null) service.refresh(false);
          });
      row.addView(toggle);
      Button settings = new Button(this);
      settings.setText("Cài đặt banner / thông báo");
      settings.setOnClickListener(
          v -> {
            try {
              startActivity(
                  new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                      .putExtra(Settings.EXTRA_APP_PACKAGE, pkg));
            } catch (RuntimeException ex) {
              Toast.makeText(this, "Không mở được cài đặt ứng dụng", Toast.LENGTH_SHORT).show();
            }
          });
      row.addView(settings);
      rows.addView(row);
    }
  }
}
