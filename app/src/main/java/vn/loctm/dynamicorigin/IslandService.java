package vn.loctm.dynamicorigin;

import android.animation.ValueAnimator;
import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.*;

public class IslandService extends Service {
  private static java.lang.ref.WeakReference<IslandService> reference =
      new java.lang.ref.WeakReference<>(null);

  static IslandService getInstance() {
    return reference.get();
  }

  private WindowManager wm;
  private WindowManager.LayoutParams params;
  private LinearLayout root;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private boolean expanded, attached;
  private ValueAnimator animator;
  private int cameraBottom, cameraCenter = -1, cameraWidth;
  private final Runnable collapse =
      () -> {
        expanded = false;
        render();
      };
  private final BroadcastReceiver screen =
      new BroadcastReceiver() {
        public void onReceive(Context c, Intent i) {
          if (root == null) return;
          root.setVisibility(
              Intent.ACTION_SCREEN_OFF.equals(i.getAction()) || locked()
                  ? View.GONE
                  : View.VISIBLE);
        }
      };

  private boolean locked() {
    return ((KeyguardManager) getSystemService(KEYGUARD_SERVICE)).isKeyguardLocked();
  }

  int dp(float n) {
    return Math.round(n * getResources().getDisplayMetrics().density);
  }

  @Override
  public void onCreate() {
    super.onCreate();
    reference = new java.lang.ref.WeakReference<>(this);
    wm = getSystemService(WindowManager.class);
    Inbox.load(this);
    IntentFilter f = new IntentFilter();
    f.addAction(Intent.ACTION_SCREEN_OFF);
    f.addAction(Intent.ACTION_SCREEN_ON);
    f.addAction(Intent.ACTION_USER_PRESENT);
    registerReceiver(screen, f, RECEIVER_NOT_EXPORTED);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int id) {
    if (!Prefs.enabled(this) || !Settings.canDrawOverlays(this)) {
      stopSelf();
      return START_NOT_STICKY;
    }
    try {
      NotificationManager nm = getSystemService(NotificationManager.class);
      nm.createNotificationChannel(
          new NotificationChannel("runtime", "Hoạt động nền", NotificationManager.IMPORTANCE_LOW));
      PendingIntent open =
          PendingIntent.getActivity(
              this,
              0,
              new Intent(this, MainActivity.class),
              PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
      PendingIntent stop =
          PendingIntent.getService(
              this,
              1,
              new Intent(this, IslandService.class).setAction("stop"),
              PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
      if (intent != null && "stop".equals(intent.getAction())) {
        Prefs.get(this).edit().putBoolean("enabled", false).apply();
        stopSelf();
        return START_NOT_STICKY;
      }
      Notification n =
          new Notification.Builder(this, "runtime")
              .setSmallIcon(R.drawable.ic_island)
              .setContentTitle("DynAmic Origin đang hoạt động")
              .setContentText("Chạm để căn chỉnh đảo và quản lý thông báo")
              .setContentIntent(open)
              .setOngoing(true)
              .addAction(new Notification.Action.Builder(null, "Tắt đảo", stop).build())
              .build();
      startForeground(17, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
      boolean fresh = root == null;
      if (fresh) attach();
      else render();
      if (fresh && NotificationBridge.getInstance() != null)
        NotificationBridge.getInstance().replay();
      Prefs.get(this).edit().remove("error").apply();
    } catch (RuntimeException ex) {
      Prefs.get(this).edit().putString("error", ex.toString()).apply();
      stopSelf();
      return START_NOT_STICKY;
    }
    return START_STICKY;
  }

  private void attach() {
    root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setElevation(dp(8));
    GradientDrawable bg = new GradientDrawable();
    bg.setColor(Color.BLACK);
    bg.setCornerRadius(dp(28));
    root.setBackground(bg);
    root.setClipToOutline(true);
    params =
        new WindowManager.LayoutParams(
            dp(120),
            dp(36),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            android.graphics.PixelFormat.TRANSLUCENT);
    params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
    params.layoutInDisplayCutoutMode =
        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
    root.setOnApplyWindowInsetsListener(
        (v, insets) -> {
          DisplayCutout c = insets.getDisplayCutout();
          if (c != null) {
            for (android.graphics.Rect b : c.getBoundingRects())
              if (b.top < dp(100)) {
                int old = cameraBottom;
                cameraBottom = b.bottom;
                cameraCenter = b.centerX();
                cameraWidth = b.width();
                if (old != cameraBottom) handler.post(() -> render());
                break;
              }
          }
          return insets;
        });
    wm.addView(root, params);
    attached = true;
    root.requestApplyInsets();
    root.setVisibility(locked() ? View.GONE : View.VISIBLE);
    render();
  }

  boolean ready() {
    return attached && root != null && root.getVisibility() == View.VISIBLE;
  }

  void refresh(boolean alert) {
    handler.post(
        () -> {
          if (root == null) return;
          if (alert && !locked()) expanded = true;
          render();
          handler.removeCallbacks(collapse);
          if (alert) handler.postDelayed(collapse, 6500);
        });
  }

  private TextView text(String s, int size, int color) {
    TextView t = new TextView(this);
    t.setText(s);
    t.setTextColor(color);
    t.setTextSize(size);
    return t;
  }

  private void pillTouch(View view) {
    view.setOnClickListener(
        v -> {
          java.util.List<Entry> all = Inbox.list(IslandService.this);
          if (!all.isEmpty()) open(all.get(0));
          else {
            expanded = true;
            render();
          }
        });
    GestureDetector gesture =
        new GestureDetector(
            this,
            new GestureDetector.SimpleOnGestureListener() {
              public boolean onDown(android.view.MotionEvent e) {
                return true;
              }

              public boolean onSingleTapUp(android.view.MotionEvent e) {
                view.performClick();
                return true;
              }

              public void onLongPress(android.view.MotionEvent e) {
                expanded = !expanded;
                handler.removeCallbacks(collapse);
                render();
              }

              public boolean onFling(
                  android.view.MotionEvent a, android.view.MotionEvent b, float vx, float vy) {
                if (a != null && b.getY() < a.getY() - dp(10)) {
                  expanded = false;
                  render();
                  return true;
                }
                return false;
              }
            });
    view.setOnTouchListener((v, e) -> gesture.onTouchEvent(e));
  }

  private void render() {
    if (root == null || !attached) return;
    root.removeAllViews();
    int sw = wm.getCurrentWindowMetrics().getBounds().width(),
        sh = wm.getCurrentWindowMetrics().getBounds().height();
    int available = sw - dp(16);
    int idle =
        Math.min(
            available, Math.max(cameraWidth + dp(30), sw * Prefs.number(this, "width", 30) / 100));
    int h =
        Math.max(
            dp(Prefs.number(this, "height", 36)),
            cameraBottom > 0
                ? cameraBottom - Math.max(0, dp(Prefs.number(this, "offset", 8))) + dp(4)
                : 0);
    h = Math.min(dp(96), h);
    params.y = Math.max(0, dp(Prefs.number(this, "offset", 8)));
    params.x = cameraCenter > 0 ? cameraCenter - sw / 2 : 0;
    java.util.List<Entry> list = Inbox.list(this);
    LinearLayout header = new LinearLayout(this);
    header.setGravity(Gravity.CENTER_VERTICAL);
    header.setPadding(dp(12), 0, dp(12), 0);
    TextView left = text(list.isEmpty() ? "" : "●", 13, 0xff70caff);
    header.addView(left, new LinearLayout.LayoutParams(0, h, 1));
    View gap = new View(this);
    header.addView(gap, new LinearLayout.LayoutParams(Math.max(cameraWidth + dp(12), dp(32)), h));
    TextView count = text(list.isEmpty() ? "" : String.valueOf(list.size()), 12, Color.WHITE);
    count.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
    header.addView(count, new LinearLayout.LayoutParams(0, h, 1));
    root.addView(header);
    pillTouch(header);
    if (expanded) {
      LinearLayout bar = new LinearLayout(this);
      bar.setPadding(dp(16), dp(4), dp(12), dp(4));
      TextView title = text("Thông báo", 17, Color.WHITE);
      bar.addView(title, new LinearLayout.LayoutParams(0, dp(38), 1));
      TextView close = text("Thu gọn", 13, 0xff70caff);
      close.setGravity(Gravity.CENTER);
      close.setOnClickListener(
          v -> {
            expanded = false;
            render();
          });
      bar.addView(close);
      root.addView(bar);
      ScrollView scroll = new ScrollView(this);
      LinearLayout rows = new LinearLayout(this);
      rows.setOrientation(LinearLayout.VERTICAL);
      rows.setPadding(dp(16), 0, dp(16), dp(12));
      scroll.addView(rows);
      root.addView(scroll);
      if (list.isEmpty()) rows.addView(text("Chưa có thông báo. Đảo đang chờ.", 14, 0xffaaaaaa));
      for (Entry e : list) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(10), 0, dp(10));
        TextView app =
            text(e.app + (e.removed ? " · đã lưu" : " · đang hoạt động"), 11, 0xff83bfff);
        row.addView(app);
        TextView t = text(e.title, 15, Color.WHITE);
        t.setMaxLines(2);
        row.addView(t);
        TextView body = text(e.text, 13, 0xffcccccc);
        body.setMaxLines(4);
        row.addView(body);
        t.setOnClickListener(v -> open(e));
        body.setOnClickListener(v -> open(e));
        LinearLayout actions = new LinearLayout(this);
        Button open = new Button(this);
        open.setText("Mở");
        open.setOnClickListener(v -> open(e));
        actions.addView(open);
        Button del = new Button(this);
        del.setText("Xóa");
        del.setOnClickListener(
            v -> {
              NotificationBridge.dismiss(e.key);
              Inbox.remove(this, e.key);
              render();
            });
        actions.addView(del);
        row.addView(actions);
        if (e.media != null && !e.removed) {
          LinearLayout controls = new LinearLayout(this);
          android.media.session.MediaController mc =
              new android.media.session.MediaController(this, e.media);
          for (int i = 0; i < 3; i++) {
            final int which = i;
            Button b = new Button(this);
            b.setText(new String[] {"Trước", "▶ / Ⅱ", "Sau"}[i]);
            b.setOnClickListener(
                v -> {
                  try {
                    android.media.session.MediaController.TransportControls tc =
                        mc.getTransportControls();
                    if (which == 0) tc.skipToPrevious();
                    else if (which == 2) tc.skipToNext();
                    else {
                      android.media.session.PlaybackState st = mc.getPlaybackState();
                      if (st != null
                          && st.getState() == android.media.session.PlaybackState.STATE_PLAYING)
                        tc.pause();
                      else tc.play();
                    }
                  } catch (RuntimeException ex) {
                    toast("Ứng dụng không cho phép điều khiển");
                  }
                });
            controls.addView(b, new LinearLayout.LayoutParams(0, dp(48), 1));
          }
          row.addView(controls);
        }
        if (e.actions != null && !e.removed)
          for (Notification.Action a : e.actions) {
            if (a.actionIntent == null) continue;
            Button b = new Button(this);
            b.setText(a.title);
            b.setOnClickListener(
                v -> {
                  if (a.getRemoteInputs() != null) {
                    toast("Mở ứng dụng để trả lời");
                    open(e);
                  } else send(a.actionIntent, e.pkg);
                });
            row.addView(b);
          }
        rows.addView(row);
        View line = new View(this);
        line.setBackgroundColor(0xff24242a);
        rows.addView(line, new LinearLayout.LayoutParams(-1, dp(1)));
      }
    }
    int targetWidth = expanded ? Math.min(available, dp(440)) : idle;
    int targetHeight = h;
    if (expanded) {
      root.measure(
          View.MeasureSpec.makeMeasureSpec(targetWidth, View.MeasureSpec.EXACTLY),
          View.MeasureSpec.makeMeasureSpec(
              Math.max(dp(200), sh * 55 / 100), View.MeasureSpec.AT_MOST));
      targetHeight = root.getMeasuredHeight();
    }
    resize(targetWidth, targetHeight);
  }

  private void resize(int w, int h) {
    if (animator != null) animator.cancel();
    int ow = params.width, oh = params.height;
    animator = ValueAnimator.ofFloat(0, 1);
    animator.setDuration(220);
    animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
    animator.addUpdateListener(
        a -> {
          float f = (float) a.getAnimatedValue();
          params.width = (int) (ow + (w - ow) * f);
          params.height = (int) (oh + (h - oh) * f);
          if (attached)
            try {
              wm.updateViewLayout(root, params);
            } catch (RuntimeException ex) {
              attached = false;
              stopSelf();
            }
        });
    animator.start();
  }

  private void toast(String s) {
    Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
  }

  private void send(PendingIntent pi, String pkg) {
    try {
      ActivityOptions options = ActivityOptions.makeBasic();
      options.setPendingIntentBackgroundActivityStartMode(
          ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
      pi.send(this, 0, null, null, null, null, options.toBundle());
    } catch (Exception ex) {
      launch(pkg);
    }
  }

  private void open(Entry e) {
    expanded = false;
    render();
    if (e.open != null) send(e.open, e.pkg);
    else launch(e.pkg);
  }

  private void launch(String pkg) {
    Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
    if (i != null)
      try {
        startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
      } catch (RuntimeException ex) {
        toast("Không mở được ứng dụng");
      }
    else toast("Mở ứng dụng từ màn hình chính");
  }

  @Override
  public void onConfigurationChanged(Configuration c) {
    super.onConfigurationChanged(c);
    cameraCenter = -1;
    cameraBottom = 0;
    cameraWidth = 0;
    if (root != null) root.requestApplyInsets();
    expanded = false;
    render();
  }

  @Override
  public void onDestroy() {
    handler.removeCallbacksAndMessages(null);
    if (animator != null) animator.cancel();
    if (attached && root != null) wm.removeView(root);
    attached = false;
    reference.clear();
    unregisterReceiver(screen);
    super.onDestroy();
  }

  @Override
  public IBinder onBind(Intent i) {
    return null;
  }
}
