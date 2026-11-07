package vn.loctm.originbridge;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

public final class MainActivity extends Activity {
  private LinearLayout root;
  private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }
  private TextView label(String text, int size, boolean bold) {
    TextView t = new TextView(this);
    t.setText(text); t.setTextSize(size); t.setTextColor(Color.WHITE);
    if (bold) t.setTypeface(null, Typeface.BOLD);
    t.setPadding(dp(4),dp(9),dp(4),dp(9));
    return t;
  }
  private void action(String title, String description, Runnable callback) {
    LinearLayout card = new LinearLayout(this);
    card.setOrientation(LinearLayout.VERTICAL);
    card.setPadding(dp(18),dp(10),dp(18),dp(10));
    GradientDrawable bg = new GradientDrawable();
    bg.setColor(0xff202638); bg.setCornerRadius(dp(22));
    card.setBackground(bg);
    card.addView(label(title,17,true));
    TextView sub = label(description,13,false); sub.setTextColor(0xffc1cad9); card.addView(sub);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2);
    lp.bottomMargin=dp(12); root.addView(card,lp);
    card.setOnClickListener(v -> callback.run());
  }
  @Override public void onCreate(Bundle state) { super.onCreate(state); show(); }
  @Override public void onResume() { super.onResume(); }
  private void show() {
    ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(0xff0d111c);
    root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(22),dp(48),dp(22),dp(32)); scroll.addView(root);
    root.addView(label("LTM Origin Bridge",30,true));
    root.addView(label("Native Origin Island · OriginOS 6\nỨng dụng mới độc lập. Không sử dụng overlay hoặc trợ năng.",14,false));
    action("1 · Cấp quyền thông báo",
      "Cho phép ứng dụng gửi thẻ thử nghiệm qua Android NotificationManager.",
      () -> {
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
          requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},11);
        else startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()));
      });
    action("2 · Cấp quyền đọc thông báo",
      "Cho phép cầu nối quan sát thông báo từ ứng dụng khác. Chưa tự động chuyển tiếp.",
      () -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
    action("3 · Thử Origin Island gốc",
      "Gửi một thẻ chẩn đoán riêng. SystemUI có thể chỉ hiển thị trong khay thông báo.",
      () -> {
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
          requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},11); return;
        }
        NativeProbe.show(this);
        Toast.makeText(this,"Đã gửi thông báo thử nghiệm; hãy kiểm tra kết quả trên thiết bị.",Toast.LENGTH_LONG).show();
      });
    action("4 · Gỡ thẻ thử nghiệm","Chỉ hủy thông báo chẩn đoán của LTM Origin Bridge.",
      () -> { NativeProbe.clear(this); Toast.makeText(this,"Đã yêu cầu hủy thẻ.",Toast.LENGTH_SHORT).show(); });
    root.addView(label("TRẠNG THÁI\nGiao thức OEM chưa được xác minh trên thiết bị. Không thể bảo đảm đảo gốc xuất hiện, tồn tại liên tục hoặc hoạt động trên màn hình khóa.",13,false));
    setContentView(scroll);
  }
}
