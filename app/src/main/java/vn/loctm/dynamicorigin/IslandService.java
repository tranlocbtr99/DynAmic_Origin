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
  private boolean expanded, attached, replyVisible;
  private Entry primaryEntry;
  private boolean recentMode, recentLoading;
  private String detailKey;
  private ScrollView panelScroll;
  private boolean resetScroll;
  private int visibleNotifications = 8, recentRequest;
  private java.util.List<RecentApps.App> recentApps = java.util.Collections.emptyList();
  private final java.util.concurrent.ExecutorService recentWorker =
      java.util.concurrent.Executors.newSingleThreadExecutor();
  private ValueAnimator animator;
  private int cameraBottom, cameraCenter = -1, cameraWidth;
  private int screenWidth,
      screenHeight,
      screenDensity,
      screenRotation,
      animationTargetW = -1,
      animationTargetH = -1;
  private final Runnable collapse =
      () -> {
        expanded = false;
        recentMode = false;
        render();
      };
  private final BroadcastReceiver screen =
      new BroadcastReceiver() {
        public void onReceive(Context c, Intent i) {
          if (root == null) return;
          root.setVisibility(
              Intent.ACTION_SCREEN_OFF.equals(i.getAction()) || locked() || replyVisible
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
    root.setElevation(dp(4));
    GradientDrawable bg = new GradientDrawable();
    bg.setColor(Color.BLACK);
    bg.setCornerRadius(dp(28));
    bg.setStroke(1, 0x20ffffff);
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
    params.gravity = Gravity.TOP | Gravity.LEFT;
    params.setFitInsetsTypes(0);
    params.setFitInsetsIgnoringVisibility(true);
    readScreenAnchor();
    params.x =
        IslandGeometry.left(
            IslandGeometry.center(
                screenWidth, cameraCenter, dp(Prefs.number(this, "horizontal", 0))),
            params.width);
    params.y = Math.max(0, dp(Prefs.number(this, "offset", 8)));
    params.layoutInDisplayCutoutMode =
        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
    wm.addView(root, params);
    attached = true;

    root.setVisibility(locked() ? View.GONE : View.VISIBLE);
    render();
  }

  boolean ready() {
    return attached && root != null && root.getVisibility() == View.VISIBLE;
  }

  void showInbox() {
    recentMode = false;
    expanded = true;
    handler.removeCallbacks(collapse);
    render();
  }

  void refresh(boolean alert) {
    handler.post(
        () -> {
          if (root == null) return;
          if (alert && !locked() && !(recentMode && expanded)) {
            recentMode = false;
            expanded = true;
            visibleNotifications = 8;
            resetScroll = true;
          }
          render();
          if (alert && !recentMode) {
            handler.removeCallbacks(collapse);
            handler.postDelayed(collapse, Prefs.number(this, "duration", 7) * 1000L);
          }
        });
  }

  private TextView text(String s, int size, int color) {
    TextView t = new TextView(this);
    t.setText(s);
    t.setTextColor(color);
    IOSStyle.text(t, size, false, color);
    return t;
  }

  private void pillTouch(View view) {
    view.setOnClickListener(
        v -> {
          java.util.List<Entry> all = Inbox.list(IslandService.this);
          if (!all.isEmpty()) open(all.get(0));
          else {
            recentMode = false;
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
                if (IslandTap.right(e.getX(), view.getWidth())) showRecentApps();
                else view.performClick();
                return true;
              }

              public void onLongPress(android.view.MotionEvent e) {
                recentMode = false;
                expanded = !expanded;
                handler.removeCallbacks(collapse);
                render();
              }

              public boolean onFling(
                  android.view.MotionEvent a, android.view.MotionEvent b, float vx, float vy) {
                if (a != null && b.getY() < a.getY() - dp(10)) {
                  expanded = false;
                  recentMode = false;
                  render();
                  return true;
                }
                return false;
              }
            });
    view.setContentDescription(
        "Chạm trái mở thông báo mới nhất, chạm phải mở ứng dụng gần đây, nhấn giữ mở danh sách"
            + " thông báo");
    view.setOnTouchListener((v, e) -> gesture.onTouchEvent(e));
  }

  private void render() {
    if (root == null || !attached) return;
    final int oldScroll = resetScroll || panelScroll == null ? 0 : panelScroll.getScrollY();
    resetScroll = false;
    panelScroll = null;
    root.removeAllViews();
    int sw = screenWidth, sh = screenHeight;
    int center = IslandGeometry.center(sw, cameraCenter, dp(Prefs.number(this, "horizontal", 0)));
    int available = IslandGeometry.maxWidth(sw, center, dp(8));
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

    java.util.List<Entry> list = Inbox.list(this);
    LinearLayout header = new LinearLayout(this);
    header.setGravity(Gravity.CENTER_VERTICAL);
    header.setPadding(dp(14), 0, dp(14), 0);
    TextView left = text(list.isEmpty() ? "" : "●", 13, 0xff70caff);
    left.setGravity(Gravity.CENTER_VERTICAL);
    header.addView(left, new LinearLayout.LayoutParams(0, h, 1));
    View gap = new View(this);
    header.addView(gap, new LinearLayout.LayoutParams(Math.max(cameraWidth + dp(12), dp(32)), h));
    TextView count = text("▦", 18, 0xffd1d1d6);
    count.setContentDescription("Ứng dụng gần đây");
    count.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
    header.addView(count, new LinearLayout.LayoutParams(0, h, 1));
    java.util.List<Entry> activities = new java.util.ArrayList<>();
    for (Entry e : list) if (e.ongoing && !e.removed) activities.add(e);
    primaryEntry = activities.isEmpty() ? null : activities.get(0);
    if (primaryEntry != null) {
      left.setText(compactLabel(primaryEntry));
      left.setSingleLine(true);
      left.setGravity(Gravity.CENTER_VERTICAL);
      left.setEllipsize(android.text.TextUtils.TruncateAt.END);
    }
    root.addView(header);
    pillTouch(header);
    if (expanded) {
      LinearLayout bar = new LinearLayout(this);
      bar.setPadding(dp(22), dp(6), dp(14), dp(12));
      TextView title = text(recentMode ? "Ứng dụng gần đây" : "Thông báo", 20, Color.WHITE);
      title.setTypeface(
          android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
      bar.addView(title, new LinearLayout.LayoutParams(0, dp(38), 1));
      TextView close = text("⌃", 23, 0xffb8b8c0);
      close.setContentDescription("Thu gọn");
      close.setBackground(new IOSStyle.Glass(this, true, 22));
      close.setMinWidth(dp(44));
      close.setMinHeight(dp(44));
      close.setGravity(Gravity.CENTER);
      close.setOnClickListener(
          v -> {
            expanded = false;
            recentMode = false;
            render();
          });
      bar.addView(close);
      root.addView(bar);
      ScrollView scroll = new ScrollView(this);
      panelScroll = scroll;
      scroll.post(() -> scroll.scrollTo(0, oldScroll));
      LinearLayout rows = new LinearLayout(this);
      rows.setOrientation(LinearLayout.VERTICAL);
      rows.setPadding(dp(14), 0, dp(14), dp(16));
      scroll.addView(rows);
      root.addView(scroll);
      if (recentMode) {
        recentRows(rows);
      } else {
        if (list.isEmpty()) rows.addView(text("Chưa có thông báo. Đảo đang chờ.", 14, 0xffaaaaaa));
        for (Entry e : list.subList(0, Math.min(visibleNotifications, list.size()))) {
          boolean details = e.key.equals(detailKey);
          LinearLayout row = new LinearLayout(this);
          row.setOrientation(LinearLayout.VERTICAL);
          row.setPadding(dp(14), dp(10), dp(14), dp(10));
          row.setOnClickListener(v -> toggleDetails(e.key));
          row.setBackground(IOSStyle.solid(this, 0xff141416, 22));
          TextView app = text(e.app, 12, 0xffb0b0b8);
          LinearLayout meta = new LinearLayout(this);
          meta.setGravity(Gravity.CENTER_VERTICAL);
          if (e.icon != null) {
            ImageView icon = new ImageView(this);
            icon.setImageDrawable(e.icon);
            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(20), dp(20));
            ip.setMarginEnd(dp(9));
            meta.addView(icon, ip);
          }
          app.setSingleLine(true);
          app.setEllipsize(android.text.TextUtils.TruncateAt.END);
          meta.addView(app, new LinearLayout.LayoutParams(0, dp(22), 1));
          TextView age =
              text(
                  android.text.format.DateUtils.getRelativeTimeSpanString(
                          e.time, System.currentTimeMillis(), 60000)
                      .toString(),
                  10,
                  0xff8e8e93);
          meta.addView(age);
          row.addView(meta);
          TextView t = text(e.title, 17, Color.WHITE);
          t.setTypeface(
              android.graphics.Typeface.create(
                  "sans-serif-medium", android.graphics.Typeface.NORMAL));
          t.setPadding(0, dp(5), 0, dp(3));
          t.setMaxLines(details ? 3 : 1);
          t.setEllipsize(android.text.TextUtils.TruncateAt.END);
          row.addView(t);
          TextView body = text(e.text, 15, 0xffc5c5cc);
          body.setLineSpacing(dp(3), 1);
          body.setMaxLines(details ? 8 : 1);
          body.setEllipsize(android.text.TextUtils.TruncateAt.END);
          row.addView(body);
          if (!e.removed && (e.progressMax > 0 || e.indeterminate)) {
            ProgressBar progress =
                new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            progress.setIndeterminate(e.indeterminate);
            if (!e.indeterminate) {
              progress.setMax(e.progressMax);
              progress.setProgress(Math.max(0, Math.min(e.progressMax, e.progress)));
            }
            row.addView(progress, new LinearLayout.LayoutParams(-1, dp(8)));
          }
          t.setOnClickListener(v -> toggleDetails(e.key));
          body.setOnClickListener(v -> toggleDetails(e.key));
          if (details) {
            LinearLayout actions = new LinearLayout(this);
            Button open = IOSStyle.button(this, "Mở", false, true);
            open.setOnClickListener(v -> open(e));
            LinearLayout.LayoutParams openParams = new LinearLayout.LayoutParams(0, dp(42), 1);
            openParams.setMarginEnd(dp(8));
            actions.addView(open, openParams);
            Button del = IOSStyle.button(this, "Xóa", false, true);
            del.setOnClickListener(
                v -> {
                  NotificationBridge.dismiss(e.key);
                  Inbox.remove(this, e.key);
                  render();
                });
            actions.addView(del, new LinearLayout.LayoutParams(0, dp(42), 1));
            LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(-1, -2);
            actionParams.topMargin = dp(14);
            row.addView(actions, actionParams);
            if (e.media != null && !e.removed) {
              LinearLayout controls = new LinearLayout(this);
              android.media.session.MediaController mc =
                  new android.media.session.MediaController(this, e.media);
              for (int i = 0; i < 3; i++) {
                final int which = i;
                Button b = IOSStyle.button(this, new String[] {"⏮", "▶ / Ⅱ", "⏭"}[i], false, true);
                b.setContentDescription(
                    new String[] {"Bài trước", "Phát hoặc tạm dừng", "Bài tiếp theo"}[i]);
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
                LinearLayout.LayoutParams controlParams =
                    new LinearLayout.LayoutParams(0, dp(46), 1);
                controlParams.setMargins(dp(3), dp(10), dp(3), 0);
                controls.addView(b, controlParams);
              }
              row.addView(controls);
            }
            if (e.actions != null && (!e.removed || e.transferred))
              for (Notification.Action a : e.actions) {
                if (a.actionIntent == null) continue;
                Button b = IOSStyle.button(this, a.title.toString(), false, true);
                b.setOnClickListener(
                    v -> {
                      if (a.getRemoteInputs() != null) {
                        startReply(e, a);
                      } else send(a.actionIntent, e.pkg);
                    });
                LinearLayout.LayoutParams actionLayout = new LinearLayout.LayoutParams(-1, dp(42));
                actionLayout.topMargin = dp(8);
                row.addView(b, actionLayout);
              }
          }
          rows.addView(row);
          View line = new View(this);
          rows.addView(line, new LinearLayout.LayoutParams(-1, dp(10)));
        }
        if (visibleNotifications < list.size()) {
          Button more =
              IOSStyle.button(
                  this, "Xem thêm · " + (list.size() - visibleNotifications), false, true);
          more.setOnClickListener(
              v -> {
                visibleNotifications += 8;
                render();
              });
          rows.addView(more, new LinearLayout.LayoutParams(-1, dp(44)));
        }
      }
    }
    int targetWidth =
        expanded
            ? Math.min(available, dp(440))
            : activities.isEmpty() ? idle : Math.min(available, Math.max(idle, sw * 48 / 100));
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

  private void toggleDetails(String key) {
    detailKey = key.equals(detailKey) ? null : key;
    handler.removeCallbacks(collapse);
    render();
  }

  private void showRecentApps() {
    resetScroll = true;
    if (locked()) return;
    recentMode = true;
    expanded = true;
    recentLoading = RecentApps.permitted(this);
    recentApps = java.util.Collections.emptyList();
    handler.removeCallbacks(collapse);
    render();
    final int request = ++recentRequest;
    if (!recentLoading) return;
    recentWorker.execute(
        () -> {
          java.util.List<RecentApps.App> result;
          try {
            result = RecentApps.query(this);
          } catch (RuntimeException ex) {
            result = java.util.Collections.emptyList();
          }
          final java.util.List<RecentApps.App> completed = result;
          handler.post(
              () -> {
                if (!attached || request != recentRequest) return;
                recentLoading = false;
                recentApps = completed;
                if (recentMode && expanded) render();
              });
        });
  }

  private void recentRows(LinearLayout rows) {
    if (!RecentApps.permitted(this)) {
      TextView help =
          text(
              "Cho phép truy cập dữ liệu sử dụng để xem các ứng dụng vừa mở. Không cần trợ năng.",
              14,
              0xffb0b0b8);
      help.setPadding(dp(8), dp(8), dp(8), dp(14));
      rows.addView(help);
      Button allow = IOSStyle.button(this, "Cấp quyền truy cập sử dụng", true, true);
      allow.setOnClickListener(
          v -> {
            expanded = false;
            recentMode = false;
            render();
            try {
              startActivity(
                  new Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
                      .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (RuntimeException ex) {
              toast("Mở Truy cập dữ liệu sử dụng trong Cài đặt");
            }
          });
      rows.addView(allow, new LinearLayout.LayoutParams(-1, dp(46)));
      return;
    }
    if (recentLoading) {
      rows.addView(text("Đang lấy ứng dụng gần đây…", 14, 0xffaaaaaa));
      return;
    }
    if (recentApps.isEmpty())
      rows.addView(
          text(
              "Chưa có ứng dụng gần đây trong 24 giờ qua. Nếu vừa cấp quyền, hãy mở vài ứng dụng"
                  + " rồi chạm phải đảo lần nữa.",
              14,
              0xffaaaaaa));
    for (RecentApps.App app : recentApps) {
      LinearLayout row = new LinearLayout(this);
      row.setGravity(Gravity.CENTER_VERTICAL);
      row.setPadding(dp(14), dp(10), dp(14), dp(10));
      row.setBackground(new IOSStyle.Glass(this, true, 22));
      ImageView icon = new ImageView(this);
      icon.setImageDrawable(app.icon);
      LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(36), dp(36));
      ip.setMarginEnd(dp(12));
      row.addView(icon, ip);
      TextView name = text(app.name, 16, Color.WHITE);
      name.setSingleLine(true);
      name.setEllipsize(android.text.TextUtils.TruncateAt.END);
      row.addView(name, new LinearLayout.LayoutParams(0, dp(40), 1));
      name.setGravity(Gravity.CENTER_VERTICAL);
      row.addView(text("›", 24, 0xffaaaaaa));
      row.setContentDescription("Mở " + app.name);
      row.setOnClickListener(
          v -> {
            expanded = false;
            recentMode = false;
            render();
            launch(app.pkg);
          });
      LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
      rp.bottomMargin = dp(8);
      rows.addView(row, rp);
    }
  }

  private String compactLabel(Entry e) {
    if (!e.removed && e.progressMax > 0)
      return Math.round(100.0 * e.progress / e.progressMax) + "%";
    if (e.media != null && !e.removed) return "♫";
    return e.app.isEmpty() ? "●" : e.app;
  }

  void replyVisibility(boolean visible) {
    replyVisible = visible;
    if (root != null)
      root.setVisibility(
          visible || locked() || !getSystemService(PowerManager.class).isInteractive()
              ? View.GONE
              : View.VISIBLE);
  }

  private void startReply(Entry e, Notification.Action action) {
    int index = java.util.Arrays.asList(e.actions).indexOf(action);
    expanded = false;
    render();
    replyVisibility(true);
    try {
      startActivity(
          new Intent(this, ReplyActivity.class)
              .putExtra("key", e.key)
              .putExtra("index", index)
              .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    } catch (RuntimeException ex) {
      replyVisibility(false);
      toast("Không mở được trả lời nhanh");
    }
  }

  private void resize(int w, int h) {
    if (animationTargetW == w && animationTargetH == h && animator != null && animator.isRunning())
      return;
    if (animator != null) animator.cancel();
    animationTargetW = w;
    animationTargetH = h;
    if (params.width == w && params.height == h) {
      applyWindowFrame(w, h);
      return;
    }
    int ow = params.width, oh = params.height;
    animator = ValueAnimator.ofFloat(0, 1);
    animator.setDuration(ValueAnimator.areAnimatorsEnabled() ? 280 : 0);
    animator.setInterpolator(new android.view.animation.PathInterpolator(0.2f, 0.8f, 0.2f, 1f));
    animator.addUpdateListener(
        a -> {
          float f = (float) a.getAnimatedValue();
          applyWindowFrame(Math.round(ow + (w - ow) * f), Math.round(oh + (h - oh) * f));
        });
    animator.start();
  }

  private void readScreenAnchor() {
    WindowMetrics metrics = wm.getMaximumWindowMetrics();
    screenWidth = metrics.getBounds().width();
    screenHeight = metrics.getBounds().height();
    screenDensity = getResources().getDisplayMetrics().densityDpi;
    screenRotation = wm.getDefaultDisplay().getRotation();
    cameraCenter = screenWidth / 2;
    cameraWidth = 0;
    cameraBottom = 0;
    DisplayCutout cutout = metrics.getWindowInsets().getDisplayCutout();
    if (cutout != null) {
      for (android.graphics.Rect rect : cutout.getBoundingRects())
        if (rect.top < dp(100) && rect.width() < screenWidth / 2 && rect.height() > 0) {
          cameraCenter = rect.centerX();
          cameraWidth = rect.width();
          cameraBottom = rect.bottom;
          break;
        }
    }
  }

  private void applyWindowFrame(int width, int height) {
    params.width = width;
    params.height = height;
    int center =
        IslandGeometry.center(screenWidth, cameraCenter, dp(Prefs.number(this, "horizontal", 0)));
    params.x = IslandGeometry.left(center, width);
    params.y = Math.max(0, dp(Prefs.number(this, "offset", 8)));
    if (attached)
      try {
        wm.updateViewLayout(root, params);
      } catch (RuntimeException ex) {
        Prefs.get(this).edit().putString("error", ex.toString()).apply();
        stopSelf();
      }
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
    recentMode = false;
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
    WindowMetrics metrics = wm.getMaximumWindowMetrics();
    int rotation = wm.getDefaultDisplay().getRotation();
    if (screenWidth != metrics.getBounds().width()
        || screenHeight != metrics.getBounds().height()
        || screenDensity != getResources().getDisplayMetrics().densityDpi
        || screenRotation != rotation) {
      if (animator != null) animator.cancel();
      readScreenAnchor();
      animationTargetW = -1;
      animationTargetH = -1;
      expanded = false;
    }
    render();
  }

  @Override
  public void onDestroy() {
    recentWorker.shutdownNow();
    recentRequest++;
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
