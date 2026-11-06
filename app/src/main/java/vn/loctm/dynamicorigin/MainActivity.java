package vn.loctm.dynamicorigin;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.*;

public class MainActivity extends Activity {
  private LinearLayout body;

  private int dp(int n) {
    return Math.round(n * getResources().getDisplayMetrics().density);
  }

  @Override
  public void onCreate(Bundle b) {
    super.onCreate(b);
  }

  @Override
  public void onResume() {
    super.onResume();
    render();
  }

  private void label(String s, int size) {
    TextView t = new TextView(this);
    t.setText(s);
    t.setTextSize(size);
    t.setTextColor(Color.WHITE);
    t.setPadding(0, dp(12), 0, dp(8));
    body.addView(t);
  }

  private void button(String s, Runnable r) {
    Button b = new Button(this);
    b.setText(s);
    b.setAllCaps(false);
    b.setOnClickListener(v -> r.run());
    body.addView(b, new LinearLayout.LayoutParams(-1, dp(54)));
  }

  private void go(Intent i) {
    try {
      startActivity(i);
    } catch (RuntimeException e) {
      Toast.makeText(this, "Mở mục này trong Cài đặt của điện thoại", Toast.LENGTH_LONG).show();
    }
  }

  private boolean listener() {
    String s = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
    return s != null
        && java.util.Arrays.asList(s.split(":"))
            .contains(new ComponentName(this, NotificationBridge.class).flattenToString());
  }

  private void render() {
    ScrollView scroll = new ScrollView(this);
    scroll.setFillViewport(true);
    body = new LinearLayout(this);
    body.setOrientation(LinearLayout.VERTICAL);
    body.setPadding(dp(22), dp(30), dp(22), dp(24));
    body.setBackgroundColor(0xff10131c);
    scroll.addView(body);
    setContentView(scroll);
    label("DynAmic Origin", 29);
    label("Đảo quanh camera · Không cần trợ năng", 15);
    label(
        (IslandService.getInstance() != null && IslandService.getInstance().ready()
                ? "● Đảo đang hoạt động"
                : "○ Đảo chưa hiển thị")
            + "\n"
            + (NotificationBridge.getInstance() != null
                ? "Đã kết nối thông báo"
                : "Chưa kết nối thông báo"),
        16);
    String error = Prefs.get(this).getString("error", "");
    if (!error.isBlank()) label(error, 13);
    button(
        (Settings.canDrawOverlays(this) ? "✓ " : "1. ") + "Quyền hiển thị trên ứng dụng khác",
        () ->
            go(
                new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()))));
    button(
        (listener() ? "✓ " : "2. ") + "Quyền truy cập thông báo",
        () -> go(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
    button(
        "3. Cho phép thông báo dịch vụ nền",
        () -> {
          if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
              != android.content.pm.PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[] {android.Manifest.permission.POST_NOTIFICATIONS}, 9);
          else
            go(
                new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()));
        });
    button(
        "Bật đảo",
        () -> {
          if (!Settings.canDrawOverlays(this) || !listener()) {
            Toast.makeText(this, "Cấp hai quyền ở trên trước", Toast.LENGTH_LONG).show();
            return;
          }
          Prefs.get(this).edit().putBoolean("enabled", true).apply();
          startForegroundService(new Intent(this, IslandService.class));
          android.service.notification.NotificationListenerService.requestRebind(
              new ComponentName(this, NotificationBridge.class));
          body.postDelayed(this::render, 600);
        });
    button(
        "Tắt đảo",
        () -> {
          Prefs.get(this).edit().putBoolean("enabled", false).apply();
          stopService(new Intent(this, IslandService.class));
          render();
        });
    label("Căn chỉnh camera", 21);
    slider("Chiều rộng viên chờ (%)", "width", 24, 55, 30);
    slider("Chiều cao (dp)", "height", 28, 64, 36);
    slider("Khoảng cách mép trên (dp)", "offset", 0, 70, 8);
    Switch transfer = new Switch(this);
    transfer.setText(R.string.transfer_label);
    transfer.setTextColor(Color.WHITE);
    transfer.setChecked(Prefs.get(this).getBoolean("transfer", false));
    body.addView(transfer);
    transfer.setOnCheckedChangeListener(
        (v, on) -> {
          if (on)
            new AlertDialog.Builder(this)
                .setTitle("Chuyển thông báo vào đảo?")
                .setMessage(
                    "Sau khi lưu và hiển thị trên đảo, app yêu cầu xóa bản thông báo thường ở hệ"
                        + " thống. Banner có thể vẫn lóe lên. Nút thao tác của ứng dụng nguồn có"
                        + " thể hết hiệu lực. Cuộc gọi, báo thức, nhạc và dịch vụ nền được giữ"
                        + " nguyên.")
                .setPositiveButton(
                    "Bật", (d, w) -> Prefs.get(this).edit().putBoolean("transfer", true).apply())
                .setNegativeButton("Hủy", (d, w) -> transfer.setChecked(false))
                .setOnCancelListener(d -> transfer.setChecked(false))
                .show();
          else Prefs.get(this).edit().putBoolean("transfer", false).apply();
        });
    label(
        "Nhấn giữ đảo để mở hộp thư. Chạm để mở thông báo mới nhất. Vuốt lên để thu gọn. Nội dung"
            + " được ẩn khi khóa máy.",
        14);
    label("Thiết lập chạy nền", 21);
    button(
        "Mở cài đặt ứng dụng / pin / tự khởi động",
        () ->
            go(
                new Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName()))));
    button("Cài đặt banner của ứng dụng nguồn", () -> go(new Intent(Settings.ACTION_SETTINGS)));
    label(
        "Giữ quyền thông báo ứng dụng nguồn bật; tắt banner trong cài đặt kênh để giảm hiển thị"
            + " trùng. OriginOS có thể chặn lớp phủ ở thanh trạng thái hoặc màn hình bảo vệ. Không"
            + " bảo đảm hoạt động trên AOD.",
        13);
    label("Dữ liệu trên thiết bị", 21);
    button(
        "Xóa lịch sử thông báo",
        () ->
            new AlertDialog.Builder(this)
                .setMessage("Xóa toàn bộ lịch sử đã lưu trên đảo?")
                .setPositiveButton(
                    "Xóa",
                    (d, w) -> {
                      Inbox.clear(this);
                      if (IslandService.getInstance() != null)
                        IslandService.getInstance().refresh(false);
                    })
                .setNegativeButton("Hủy", null)
                .show());
    label(
        "0.1.0 · Bản thử nghiệm trên thiết bị\n"
            + "Không gửi dữ liệu lên mạng. Không có quyền trợ năng.",
        12);
  }

  private void slider(String name, String key, int min, int max, int fallback) {
    int value = Prefs.number(this, key, fallback);
    TextView l = new TextView(this);
    l.setTextColor(0xffcccccc);
    l.setText(getString(R.string.slider_value, name, value));
    body.addView(l);
    SeekBar s = new SeekBar(this);
    s.setMax(max - min);
    s.setProgress(value - min);
    body.addView(s);
    s.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          public void onStartTrackingTouch(SeekBar s) {}

          public void onStopTrackingTouch(SeekBar s) {}

          public void onProgressChanged(SeekBar s, int p, boolean user) {
            if (!user) return;
            Prefs.get(MainActivity.this).edit().putInt(key, p + min).apply();
            l.setText(getString(R.string.slider_value, name, p + min));
            if (IslandService.getInstance() != null) IslandService.getInstance().refresh(false);
          }
        });
  }
}
