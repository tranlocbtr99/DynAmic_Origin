package vn.loctm.dynamicorigin;

import android.app.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity {
  private LinearLayout body, section;
  private ScrollView pageScroll;

  private int dp(float v) {
    return IOSStyle.dp(this, v);
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

  private void go(Intent i) {
    try {
      startActivity(i);
    } catch (RuntimeException e) {
      Toast.makeText(this, "Mở mục này trong Cài đặt điện thoại", Toast.LENGTH_LONG).show();
    }
  }

  private boolean listener() {
    return getSystemService(NotificationManager.class)
        .isNotificationListenerAccessGranted(new ComponentName(this, NotificationBridge.class));
  }

  private void heading(String title) {
    TextView t = IOSStyle.text(this, title, 21, true, IOSStyle.ink(this));
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
    p.setMargins(dp(4), dp(26), 0, dp(10));
    body.addView(t, p);
    section = IOSStyle.card(this);
    body.addView(section, new LinearLayout.LayoutParams(-1, -2));
  }

  private void note(String value) {
    TextView t = IOSStyle.text(this, value, 13, false, IOSStyle.secondary(this));
    t.setLineSpacing(dp(3), 1);
    t.setPadding(dp(4), dp(10), dp(4), 0);
    body.addView(t);
  }

  private void separator() {
    View line = new View(this);
    line.setBackgroundColor(IOSStyle.dark(this) ? 0x18ffffff : 0x12000000);
    section.addView(line, new LinearLayout.LayoutParams(-1, dp(.5f)));
  }

  private void row(String title, String detail, boolean granted, Runnable action) {
    LinearLayout row = new LinearLayout(this);
    row.setGravity(Gravity.CENTER_VERTICAL);
    row.setPadding(0, dp(13), 0, dp(13));
    LinearLayout copy = IOSStyle.column(this);
    copy.addView(IOSStyle.text(this, title, 16, true, IOSStyle.ink(this)));
    if (!detail.isEmpty()) {
      TextView subtitle = IOSStyle.text(this, detail, 12, false, IOSStyle.secondary(this));
      subtitle.setPadding(0, dp(5), dp(8), 0);
      copy.addView(subtitle);
    }
    row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
    TextView arrow =
        IOSStyle.text(
            this,
            granted ? "✓" : "›",
            24,
            false,
            granted ? IOSStyle.GREEN : IOSStyle.secondary(this));
    arrow.setGravity(Gravity.CENTER);
    row.addView(arrow, new LinearLayout.LayoutParams(dp(32), dp(40)));
    row.setBackground(
        new RippleDrawable(
            ColorStateList.valueOf(0x18007aff), null, IOSStyle.solid(this, Color.WHITE, 12)));
    row.setOnClickListener(v -> action.run());
    section.addView(row);
  }

  private void render() {
    int previousScroll = pageScroll == null ? 0 : pageScroll.getScrollY();
    IOSStyle.page(this);
    FrameLayout frame = new FrameLayout(this);
    GradientDrawable wallpaper =
        new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            IOSStyle.dark(this)
                ? new int[] {0xff111321, 0xff090a10, 0xff171322}
                : new int[] {0xffe4eafa, 0xfff2f2f7, 0xffeee7f5});
    frame.setBackground(wallpaper);
    ScrollView scroll = new ScrollView(this);
    pageScroll = scroll;
    scroll.setClipToPadding(false);
    scroll.setFillViewport(true);
    body = IOSStyle.column(this);
    body.setPadding(dp(20), dp(12), dp(20), dp(108));
    scroll.addView(body);
    frame.addView(scroll);
    body.addView(IOSStyle.text(this, "DynAmic Origin", 34, true, IOSStyle.ink(this)));
    note("Đảo của bạn. Luôn ở đúng vị trí.");
    LinearLayout hero = IOSStyle.card(this);
    LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
    hp.setMargins(0, dp(24), 0, 0);
    body.addView(hero, hp);
    FrameLayout preview = new FrameLayout(this);
    preview.setBackground(IOSStyle.solid(this, IOSStyle.dark(this) ? 0xff242638 : 0xffe9edfa, 20));
    hero.addView(preview, new LinearLayout.LayoutParams(-1, dp(110)));
    View island = new View(this);
    island.setBackground(IOSStyle.solid(this, Color.BLACK, 30));
    FrameLayout.LayoutParams pill = new FrameLayout.LayoutParams(dp(126), dp(37), Gravity.CENTER);
    preview.addView(island, pill);
    TextView heroTitle = IOSStyle.text(this, "Dynamic Island", 21, true, IOSStyle.ink(this));
    heroTitle.setPadding(0, dp(16), 0, dp(7));
    hero.addView(heroTitle);
    IslandService service = IslandService.getInstance();
    boolean running = service != null && service.ready();
    TextView status =
        IOSStyle.text(
            this,
            running ? "● Đang hoạt động · Vị trí cố định" : "○ Sẵn sàng khi bạn bật đảo",
            13,
            false,
            running ? IOSStyle.GREEN : IOSStyle.secondary(this));
    hero.addView(status);
    LinearLayout actions = new LinearLayout(this);
    LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-1, dp(48));
    ap.setMargins(0, dp(18), 0, dp(4));
    hero.addView(actions, ap);
    Button start = IOSStyle.button(this, running ? "Xem thông báo" : "Bật đảo", true, false);
    actions.addView(start, new LinearLayout.LayoutParams(0, -1, 1));
    start.setOnClickListener(
        v -> {
          if (running) {
            IslandService current = IslandService.getInstance();
            if (current != null) current.showInbox();
            return;
          }
          if (!Settings.canDrawOverlays(this) || !listener()) {
            Toast.makeText(
                    this, "Cấp quyền hiển thị và thông báo bên dưới trước", Toast.LENGTH_LONG)
                .show();
            return;
          }
          Prefs.get(this).edit().putBoolean("enabled", true).apply();
          try {
            startForegroundService(new Intent(this, IslandService.class));
            android.service.notification.NotificationListenerService.requestRebind(
                new ComponentName(this, NotificationBridge.class));
            body.postDelayed(this::render, 500);
          } catch (RuntimeException e) {
            Toast.makeText(this, "Không bật được đảo. Kiểm tra quyền chạy nền.", Toast.LENGTH_LONG)
                .show();
          }
        });
    Button stop = IOSStyle.button(this, "Tắt", false, false);
    LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(dp(76), -1);
    sp.setMarginStart(dp(10));
    actions.addView(stop, sp);
    stop.setOnClickListener(
        v -> {
          Prefs.get(this).edit().putBoolean("enabled", false).apply();
          stopService(new Intent(this, IslandService.class));
          render();
        });
    String error = Prefs.get(this).getString("error", "");
    if (!error.isBlank()) note("Cần kiểm tra: " + error);
    heading("Kết nối");
    row(
        "Hiển thị trên ứng dụng khác",
        "Cho phép đảo xuất hiện quanh camera",
        Settings.canDrawOverlays(this),
        () ->
            go(
                new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()))));
    separator();
    row(
        "Truy cập thông báo",
        NotificationBridge.getInstance() != null ? "Đã kết nối" : "Nhận thông báo và nút thao tác",
        listener(),
        () -> go(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
    separator();
    row(
        "Thông báo hoạt động nền",
        "Duy trì hoạt động của đảo",
        getSystemService(NotificationManager.class).areNotificationsEnabled(),
        () -> {
          if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
              != android.content.pm.PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[] {android.Manifest.permission.POST_NOTIFICATIONS}, 9);
          else
            go(
                new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()));
        });
    row(
        "Ứng dụng gần đây",
        "Cấp quyền để dùng thao tác chạm phải đảo",
        RecentApps.permitted(this),
        () -> go(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)));
    note(
        "Chạm trái: mở thông báo mới nhất. Chạm phải: ứng dụng vừa dùng. Nhấn giữ: xem danh sách"
            + " thông báo.");
    row(
        "Đồng bộ lại thông báo",
        Prefs.get(this).getString("listener_status", "Chưa có trạng thái kết nối"),
        false,
        () -> {
          NotificationBridge.sync(this);
          Toast.makeText(
                  this,
                  "Đã yêu cầu đồng bộ. Nếu chưa kết nối, tắt rồi bật lại quyền Truy cập thông báo.",
                  Toast.LENGTH_LONG)
              .show();
          body.postDelayed(this::render, 800);
        });
    long received = Prefs.get(this).getLong("last_notification_saved", 0);
    note(
        received == 0
            ? "Chưa ghi nhận thông báo từ ứng dụng khác."
            : "Lần nhận gần nhất: "
                + Prefs.get(this).getString("last_notification_app", "")
                + " · "
                + android.text.format.DateUtils.getRelativeTimeSpanString(received));
    heading("Căn chỉnh camera");
    IOSToggle safe = new IOSToggle(this);
    safe.setChecked(Prefs.get(this).getBoolean("safe_interaction", false));
    safe.setContentDescription("Đặt dưới thanh trạng thái");
    LinearLayout safeRow = new LinearLayout(this);
    safeRow.setGravity(Gravity.CENTER_VERTICAL);
    TextView safeTitle =
        IOSStyle.text(this, "Đặt dưới thanh trạng thái", 16, true, IOSStyle.ink(this));
    safeRow.addView(safeTitle, new LinearLayout.LayoutParams(0, dp(48), 1));
    safeRow.addView(safe);
    section.addView(safeRow);
    safe.onChange(
        on -> {
          Prefs.get(this).edit().putBoolean("safe_interaction", on).apply();
          if (IslandService.getInstance() != null) IslandService.getInstance().reposition();
        });
    note(
        "Bật: đảo nằm ngay dưới thanh trạng thái để nhận chạm trên ứng dụng khác. Tắt: đặt quanh"
            + " camera; vùng này có thể bị hệ thống chặn chạm. Khi xoay ngang, đảo giữ nguyên vị"
            + " trí vật lý thay vì chuyển về giữa cạnh trên.");

    slider("Chiều rộng", "width", 8, 90, 30, "%");
    slider("Chiều cao", "height", 12, 120, 36, "dp");
    slider("Khoảng cách mép trên", "offset", 0, 240, 8, "dp");
    slider("Dịch ngang", "horizontal", -240, 240, 0, "dp");
    slider("Chiều rộng bảng mở", "expanded_width", 40, 100, 92, "%");
    slider("Độ bo góc", "radius", 0, 60, 26, "dp");
    slider("Cỡ biểu tượng", "icon_size", 12, 32, 22, "dp");
    slider("Đệm hai bên", "padding", 0, 24, 10, "dp");
    note(
        "Chạm vào con số để nhập chính xác. Kích thước thu gọn giữ nguyên cả khi có thông báo; nếu"
            + " thu rất nhỏ, nội dung có thể bị rút gọn.");
    Button reset = IOSStyle.button(this, "Đặt lại vị trí", false, false);
    LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, dp(44));
    rp.setMargins(0, dp(10), 0, dp(6));
    section.addView(reset, rp);
    reset.setOnClickListener(
        v -> {
          Prefs.get(this)
              .edit()
              .putInt("width", 30)
              .putInt("height", 36)
              .putInt("offset", 8)
              .putInt("horizontal", 0)
              .putInt("expanded_width", 92)
              .putInt("radius", 26)
              .putInt("icon_size", 22)
              .putInt("padding", 10)
              .apply();
          if (IslandService.getInstance() != null) IslandService.getInstance().refresh(false);
          render();
        });
    note(
        "Tâm đảo được giữ cố định khi mở, thu gọn hoặc nhận thông báo. Chỉnh một lần để khớp camera"
            + " của bạn.");
    heading("Vùng chạm của đảo");
    LinearLayout controlRow = new LinearLayout(this);
    controlRow.setGravity(Gravity.CENTER_VERTICAL);
    TextView controlTitle = IOSStyle.text(this, "Mở rộng vùng chạm", 16, true, IOSStyle.ink(this));
    controlTitle.setGravity(Gravity.CENTER_VERTICAL);
    controlRow.addView(controlTitle, new LinearLayout.LayoutParams(0, dp(48), 1));
    IOSToggle controls = new IOSToggle(this);
    controls.setContentDescription("Mở rộng vùng chạm trong suốt dưới đảo");
    controls.setChecked(Prefs.get(this).getBoolean("control_strip", true));
    controls.onChange(
        on -> {
          Prefs.get(this).edit().putBoolean("control_strip", on).apply();
          if (IslandService.getInstance() != null) IslandService.getInstance().refresh(false);
        });
    controlRow.addView(controls);
    section.addView(controlRow);
    slider("Mở rộng ngang", "touch_extra_width", 0, 80, 0, "dp");
    slider("Chiều cao vùng chạm", "control_height", 16, 56, 24, "dp");
    slider("Khoảng cách dưới đảo", "touch_gap", 0, 40, 0, "dp");
    note(
        "Vùng trong suốt nối ngay dưới đảo, không có nền hay chữ và không tạo thẻ thứ hai. Chạm"
            + " trái: thông báo mới nhất; chạm phải: ứng dụng gần đây; nhấn giữ: danh sách thông"
            + " báo. Phần bị thanh trạng thái che vẫn do hệ thống nhận chạm.");

    heading("Thông báo");
    LinearLayout transferRow = new LinearLayout(this);
    transferRow.setGravity(Gravity.CENTER_VERTICAL);
    TextView transferTitle = IOSStyle.text(this, "Chuyển vào đảo", 16, true, IOSStyle.ink(this));
    transferTitle.setGravity(Gravity.CENTER_VERTICAL);
    transferRow.addView(transferTitle, new LinearLayout.LayoutParams(0, dp(52), 1));
    IOSToggle toggle = new IOSToggle(this);
    toggle.setContentDescription("Chuyển thông báo thường vào đảo");
    toggle.setChecked(Prefs.get(this).getBoolean("transfer", false));
    transferRow.addView(toggle);
    section.addView(transferRow);
    toggle.onChange(
        on -> {
          if (on) {
            AlertDialog d =
                new AlertDialog.Builder(this)
                    .setTitle("Chuyển thông báo vào đảo")
                    .setMessage(
                        "Thông báo thường được lưu trên đảo trước khi yêu cầu xóa bản gốc. Banner"
                            + " hệ thống có thể vẫn xuất hiện. Cuộc gọi, báo thức và dịch vụ nền"
                            + " được giữ nguyên.")
                    .setPositiveButton(
                        "Bật",
                        (dialog, w) -> Prefs.get(this).edit().putBoolean("transfer", true).apply())
                    .setNegativeButton("Hủy", (dialog, w) -> toggle.setChecked(false))
                    .setOnCancelListener(dialog -> toggle.setChecked(false))
                    .create();
            d.show();
            IOSStyle.dialog(d);
          } else Prefs.get(this).edit().putBoolean("transfer", false).apply();
        });
    separator();
    row(
        "Ứng dụng",
        "Chọn thông báo nào xuất hiện trên đảo",
        false,
        () -> go(new Intent(this, AppRulesActivity.class)));
    separator();
    slider("Tự thu gọn sau", "duration", 3, 15, 7, "giây");
    heading("Hoạt động & dữ liệu");
    row(
        "Pin và tự khởi động",
        "Cho phép chạy nền trên OriginOS",
        false,
        () ->
            go(
                new Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName()))));
    separator();
    row(
        "Xóa lịch sử",
        "Chỉ xóa dữ liệu đã lưu trên đảo",
        false,
        () -> {
          AlertDialog d =
              new AlertDialog.Builder(this)
                  .setTitle("Xóa lịch sử thông báo?")
                  .setMessage("Thao tác này không xóa dữ liệu trong ứng dụng nguồn.")
                  .setPositiveButton(
                      "Xóa",
                      (dialog, w) -> {
                        Inbox.clear(this);
                        if (IslandService.getInstance() != null)
                          IslandService.getInstance().refresh(false);
                      })
                  .setNegativeButton("Hủy", null)
                  .create();
          d.show();
          IOSStyle.dialog(d);
        });
    note(
        "0.8.2 · Xử lý trên thiết bị · Không cần trợ năng\n"
            + "Giữ quyền thông báo của ứng dụng nguồn bật. Tắt banner của từng ứng dụng để giảm"
            + " hiển thị trùng.");
    LinearLayout dock = new LinearLayout(this);
    dock.setGravity(Gravity.CENTER);
    dock.setPadding(dp(6), dp(6), dp(6), dp(6));
    dock.setBackground(new IOSStyle.Glass(this, IOSStyle.dark(this), 32));
    dock.setElevation(dp(8));
    FrameLayout.LayoutParams dockParams =
        new FrameLayout.LayoutParams(dp(264), dp(58), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
    dockParams.bottomMargin = dp(20);
    frame.addView(dock, dockParams);
    Button tab = IOSStyle.button(this, "Đảo", true, false);
    dock.addView(tab, new LinearLayout.LayoutParams(0, -1, 1));
    tab.setOnClickListener(v -> scroll.smoothScrollTo(0, 0));
    Button apps = IOSStyle.button(this, "Ứng dụng", false, false);
    LinearLayout.LayoutParams appTab = new LinearLayout.LayoutParams(0, -1, 1);
    appTab.setMarginStart(dp(6));
    dock.addView(apps, appTab);
    apps.setOnClickListener(v -> go(new Intent(this, AppRulesActivity.class)));
    frame.setOnApplyWindowInsetsListener(
        (v, insets) -> {
          android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
          scroll.setPadding(0, bars.top, 0, 0);
          dockParams.bottomMargin = bars.bottom + dp(12);
          dock.setLayoutParams(dockParams);
          return insets;
        });
    setContentView(frame);
    scroll.post(() -> scroll.scrollTo(0, previousScroll));
  }

  private void slider(String name, String key, int min, int max, int fallback, String unit) {
    LinearLayout row = new LinearLayout(this);
    row.setGravity(Gravity.CENTER_VERTICAL);
    TextView title = IOSStyle.text(this, name, 15, false, IOSStyle.ink(this));
    title.setGravity(Gravity.CENTER_VERTICAL);
    row.addView(title, new LinearLayout.LayoutParams(0, dp(40), 1));
    TextView value =
        IOSStyle.text(
            this,
            Prefs.number(this, key, fallback) + " " + unit,
            14,
            true,
            IOSStyle.secondary(this));
    row.addView(value);
    section.addView(row);
    SeekBar seek = new SeekBar(this);
    seek.setProgressTintList(ColorStateList.valueOf(IOSStyle.BLUE));
    seek.setThumbTintList(ColorStateList.valueOf(IOSStyle.dark(this) ? Color.WHITE : 0xfffdfdff));
    seek.setProgressBackgroundTintList(
        ColorStateList.valueOf(IOSStyle.dark(this) ? 0xff363640 : 0xffdedee6));
    seek.setMax(max - min);
    seek.setProgress(Prefs.number(this, key, fallback) - min);
    section.addView(seek, new LinearLayout.LayoutParams(-1, dp(38)));
    value.setTextColor(IOSStyle.BLUE);
    value.setPadding(dp(8), dp(8), 0, dp(8));
    value.setContentDescription("Nhập " + name);
    value.setOnClickListener(
        v -> {
          EditText input = new EditText(this);
          input.setSingleLine(true);
          input.setInputType(
              android.text.InputType.TYPE_CLASS_NUMBER
                  | (min < 0 ? android.text.InputType.TYPE_NUMBER_FLAG_SIGNED : 0));
          input.setText(String.valueOf(Prefs.number(this, key, fallback)));
          input.selectAll();
          AlertDialog dialog =
              new AlertDialog.Builder(this)
                  .setTitle(name)
                  .setMessage(min + " – " + max + " " + unit)
                  .setView(input)
                  .setPositiveButton("Áp dụng", null)
                  .setNegativeButton("Hủy", null)
                  .create();
          dialog.setOnShowListener(
              d -> {
                IOSStyle.dialog(dialog);
                dialog
                    .getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(
                        b -> {
                          try {
                            int number = Integer.parseInt(input.getText().toString().trim());
                            if (number < min || number > max) {
                              input.setError("Nhập từ " + min + " đến " + max);
                              return;
                            }
                            Prefs.get(this).edit().putInt(key, number).apply();
                            seek.setProgress(number - min);
                            value.setText(number + " " + unit);
                            if (IslandService.getInstance() != null)
                              IslandService.getInstance().refresh(false);
                            dialog.dismiss();
                          } catch (NumberFormatException ex) {
                            input.setError("Nhập số nguyên hợp lệ");
                          }
                        });
              });
          dialog.show();
        });
    seek.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          public void onStartTrackingTouch(SeekBar s) {}

          public void onStopTrackingTouch(SeekBar s) {}

          public void onProgressChanged(SeekBar s, int p, boolean user) {
            if (!user) return;
            Prefs.get(MainActivity.this).edit().putInt(key, p + min).apply();
            value.setText((p + min) + " " + unit);
            if (IslandService.getInstance() != null) IslandService.getInstance().refresh(false);
          }
        });
  }
}
