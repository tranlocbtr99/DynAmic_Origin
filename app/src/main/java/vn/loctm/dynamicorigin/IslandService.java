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
  private IslandTouchLayout root;
  private FrameLayout overlayFrame;
  private IslandTouchLayout controlStrip;
  private WindowManager.LayoutParams controlParams;
  private boolean controlAttached;
  private int naturalWidth, naturalHeight, cachedNaturalWidth = -1;
  private boolean pendingRender, pendingAlert;
  private int statusBarBottom;
  private android.hardware.display.DisplayManager displays;
  private final android.hardware.display.DisplayManager.DisplayListener displayListener =
      new android.hardware.display.DisplayManager.DisplayListener() {
        public void onDisplayAdded(int id) {}

        public void onDisplayRemoved(int id) {}

        public void onDisplayChanged(int id) {
          if (id == android.view.Display.DEFAULT_DISPLAY)
            handler.post(() -> updateDisplayGeometry());
        }
      };
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
          if (Intent.ACTION_SCREEN_OFF.equals(i.getAction())) {
            expanded = false;
            recentMode = false;
            setIslandVisibility(View.GONE);
          } else {
            expanded = false;
            recentMode = false;
            setIslandVisibility(replyVisible ? View.GONE : View.VISIBLE);
            render();
            NotificationBridge.sync(IslandService.this);
          }
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
    displays = getSystemService(android.hardware.display.DisplayManager.class);
    displays.registerDisplayListener(displayListener, handler);
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
              .setVisibility(Notification.VISIBILITY_PUBLIC)
              .addAction(new Notification.Action.Builder(null, "Tắt đảo", stop).build())
              .build();
      startForeground(17, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
      boolean fresh = root == null;
      if (fresh) attach();
      else render();
      NotificationBridge.sync(this);
      Prefs.get(this).edit().remove("error").apply();
    } catch (RuntimeException ex) {
      Prefs.get(this).edit().putString("error", ex.toString()).apply();
      stopSelf();
      return START_NOT_STICKY;
    }
    return START_STICKY;
  }

  private void attach() {
    root =
        new IslandTouchLayout(
            this,
            new IslandTouchLayout.Actions() {
              public void left() {
                if (locked()) {
                  requestUnlock();
                  return;
                }
                java.util.List<Entry> list = Inbox.list(IslandService.this);
                if (list.isEmpty()) showInbox();
                else open(list.get(0));
              }

              public void right() {
                showRecentApps();
              }

              public void hold() {
                recentMode = false;
                expanded = !expanded;
                handler.removeCallbacks(collapse);
                render();
              }

              public void collapse() {
                expanded = false;
                recentMode = false;
                render();
              }

              public void finished() {
                if (pendingRender) {
                  boolean alert = pendingAlert;
                  pendingRender = false;
                  pendingAlert = false;
                  refresh(alert);
                }
              }
            });
    root.setOrientation(LinearLayout.VERTICAL);
    overlayFrame = new FrameLayout(this);
    overlayFrame.setClipChildren(false);
    overlayFrame.setClipToPadding(false);
    overlayFrame.addView(root, new FrameLayout.LayoutParams(dp(120), dp(36)));
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
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
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
    params.y = topPosition();
    params.layoutInDisplayCutoutMode =
        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
    applyWindowFrame(dp(120), dp(Prefs.number(this, "height", 36)));
    wm.addView(overlayFrame, params);
    attached = true;
    attachControlStrip();

    setIslandVisibility(
        getSystemService(PowerManager.class).isInteractive() ? View.VISIBLE : View.GONE);
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

  void reposition() {
    readScreenAnchor();
    animationTargetW = -1;
    animationTargetH = -1;
    render();
  }

  void refresh(boolean alert) {
    handler.post(
        () -> {
          if (root == null) return;
          if (root.touching() || (controlStrip != null && controlStrip.touching())) {
            pendingRender = true;
            pendingAlert |= alert;
            return;
          }
          if (alert && !(recentMode && expanded)) {
            recentMode = false;
            expanded = true;
            visibleNotifications = 8;
            resetScroll = true;
          }
          render();
          updateLockSummary();
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

  private void render() {
    if (root == null || !attached) return;
    final int oldScroll = resetScroll || panelScroll == null ? 0 : panelScroll.getScrollY();
    resetScroll = false;
    panelScroll = null;
    root.removeAllViews();
    int sw = naturalWidth, sh = naturalHeight;
    int center = IslandGeometry.center(sw, cameraCenter, dp(Prefs.number(this, "horizontal", 0)));
    int available = IslandGeometry.maxWidth(sw, center, dp(8));
    int idle = IslandSizing.width(sw, Prefs.number(this, "width", 30), available);
    int h = dp(Prefs.number(this, "height", 36));
    ((GradientDrawable) root.getBackground()).setCornerRadius(dp(Prefs.number(this, "radius", 26)));
    root.headerHeight(h);
    params.y = topPosition();
    java.util.List<Entry> list = Inbox.list(this);
    if (locked()) {
      renderLocked(available, h, idle, list.size());
      return;
    }
    LinearLayout header = new LinearLayout(this);
    header.setGravity(Gravity.CENTER_VERTICAL);
    int padding = Math.min(dp(Prefs.number(this, "padding", 10)), idle / 8);
    header.setPadding(padding, 0, padding, 0);
    TextView left = text(list.isEmpty() ? "◉" : String.valueOf(list.size()), 13, 0xff70caff);
    left.setGravity(Gravity.CENTER_VERTICAL);
    LinearLayout leading = new LinearLayout(this);
    leading.setGravity(Gravity.CENTER_VERTICAL);
    Entry newest = list.isEmpty() ? null : list.get(0);
    if (newest != null) left.setText(newest.title.isBlank() ? newest.app : newest.title);
    if (newest != null && newest.icon != null) {
      ImageView sourceIcon = new ImageView(this);
      sourceIcon.setImageDrawable(newest.icon);
      LinearLayout.LayoutParams iconSize =
          new LinearLayout.LayoutParams(
              Math.max(
                  1,
                  Math.min(dp(Prefs.number(this, "icon_size", 22)), Math.min(h - dp(2), idle / 5))),
              Math.max(
                  1,
                  Math.min(
                      dp(Prefs.number(this, "icon_size", 22)), Math.min(h - dp(2), idle / 5))));
      iconSize.setMarginEnd(dp(6));
      leading.addView(sourceIcon, iconSize);
    }
    leading.addView(left, new LinearLayout.LayoutParams(0, h, 1));
    header.addView(leading, new LinearLayout.LayoutParams(0, h, 1));
    View gap = new View(this);
    header.addView(
        gap,
        new LinearLayout.LayoutParams(
            Math.min(idle / 3, safeInteraction() ? dp(12) : Math.max(cameraWidth + dp(12), dp(32))),
            h));
    TextView count = text("▦", 18, 0xffd1d1d6);
    count.setContentDescription("Ứng dụng gần đây");
    count.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
    header.addView(count, new LinearLayout.LayoutParams(0, h, 1));
    java.util.List<Entry> activities = new java.util.ArrayList<>();
    for (Entry e : list) if (e.ongoing && !e.removed) activities.add(e);
    primaryEntry = activities.isEmpty() ? null : activities.get(0);
    if (primaryEntry != null) left.setText(compactLabel(primaryEntry));
    left.setSingleLine(true);
    left.setEllipsize(android.text.TextUtils.TruncateAt.END);
    root.addView(header);

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
            ? IslandSizing.width(sw, Prefs.number(this, "expanded_width", 92), available)
            : idle;
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
    if (locked()) {
      requestUnlock();
      return;
    }
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
    if (e.media != null && !e.removed) return "♫ " + e.title;
    return e.app.isEmpty() ? "●" : e.app;
  }

  void replyVisibility(boolean visible) {
    replyVisible = visible;
    if (root != null)
      setIslandVisibility(
          visible || !getSystemService(PowerManager.class).isInteractive()
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
    if (root.getLayoutParams().width == w && root.getLayoutParams().height == h) {
      applyWindowFrame(w, h);
      return;
    }
    int ow = root.getLayoutParams().width, oh = root.getLayoutParams().height;
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
    android.view.Display display = wm.getDefaultDisplay();
    android.util.DisplayMetrics real = new android.util.DisplayMetrics();
    display.getRealMetrics(real);
    screenWidth = real.widthPixels;
    screenHeight = real.heightPixels;
    screenDensity = real.densityDpi;
    screenRotation = display.getRotation();
    naturalWidth = (screenRotation % 2 == 0) ? screenWidth : screenHeight;
    naturalHeight = (screenRotation % 2 == 0) ? screenHeight : screenWidth;
    boolean capture = cachedNaturalWidth != naturalWidth;
    if (capture) {
      cameraCenter = naturalWidth / 2;
      cameraWidth = 0;
      cameraBottom = 0;
      cachedNaturalWidth = naturalWidth;
    }
    WindowMetrics metrics = wm.getMaximumWindowMetrics();
    statusBarBottom =
        metrics.getWindowInsets().getInsetsIgnoringVisibility(WindowInsets.Type.statusBars()).top;
    DisplayCutout cutout = display.getCutout();
    if (capture && cutout != null)
      for (android.graphics.Rect rect : cutout.getBoundingRects())
        if (rect.width() < screenWidth / 2
            && rect.height() < screenHeight / 2
            && rect.width() > 0
            && rect.height() > 0) {
          cameraCenter =
              PinnedFrame.cameraX(
                  screenRotation, naturalWidth, naturalHeight, rect.centerX(), rect.centerY());
          cameraWidth = screenRotation % 2 == 0 ? rect.width() : rect.height();
          break;
        }
    if (safeInteraction()) cameraCenter = naturalWidth / 2;
  }

  private boolean safeInteraction() {
    return Prefs.get(this).getBoolean("safe_interaction", false);
  }

  private int topPosition() {
    int manual = Math.max(0, dp(Prefs.number(this, "offset", 8)));
    return safeInteraction() ? dp(32) + manual : manual;
  }

  private void updateDisplayGeometry() {
    if (wm == null) return;
    android.view.Display display = wm.getDefaultDisplay();
    android.util.DisplayMetrics real = new android.util.DisplayMetrics();
    display.getRealMetrics(real);
    if (screenWidth == real.widthPixels
        && screenHeight == real.heightPixels
        && screenRotation == display.getRotation()
        && screenDensity == real.densityDpi) return;
    if (animator != null) animator.cancel();
    readScreenAnchor();
    animationTargetW = -1;
    animationTargetH = -1;
    expanded = false;
    recentMode = false;
    pendingRender = false;
    if (attached) render();
  }

  private void applyWindowFrame(int width, int height) {
    int center =
        IslandGeometry.center(naturalWidth, cameraCenter, dp(Prefs.number(this, "horizontal", 0)));
    int[] frame =
        PinnedFrame.map(
            screenRotation,
            naturalWidth,
            naturalHeight,
            IslandGeometry.left(center, width),
            topPosition(),
            width,
            height);
    params.x = frame[0];
    params.y = frame[1];
    params.width = frame[2];
    params.height = frame[3];
    FrameLayout.LayoutParams logical = new FrameLayout.LayoutParams(width, height);
    root.setLayoutParams(logical);
    root.setPivotX(0);
    root.setPivotY(0);
    root.setRotation(-90f * screenRotation);
    root.setTranslationX(screenRotation == 2 ? width : screenRotation == 3 ? height : 0);
    root.setTranslationY(screenRotation == 1 ? width : screenRotation == 2 ? height : 0);
    layoutControlStrip();
    if (attached)
      try {
        wm.updateViewLayout(overlayFrame, params);
      } catch (RuntimeException ex) {
        Prefs.get(this).edit().putString("error", ex.toString()).apply();
        stopSelf();
      }
  }

  private void attachControlStrip() {
    controlStrip =
        new IslandTouchLayout(
            this,
            new IslandTouchLayout.Actions() {
              public void left() {
                if (locked()) {
                  requestUnlock();
                  return;
                }
                java.util.List<Entry> all = Inbox.list(IslandService.this);
                if (all.isEmpty()) showInbox();
                else open(all.get(0));
              }

              public void right() {
                showRecentApps();
              }

              public void hold() {
                showInbox();
              }

              public void collapse() {
                expanded = false;
                recentMode = false;
                render();
              }

              public void finished() {
                if (pendingRender) {
                  boolean alert = pendingAlert;
                  pendingRender = false;
                  pendingAlert = false;
                  refresh(alert);
                }
              }
            });
    controlStrip.setOrientation(LinearLayout.HORIZONTAL);
    controlStrip.setGravity(Gravity.CENTER_VERTICAL);
    controlStrip.setPadding(dp(10), 0, dp(10), 0);
    controlStrip.setBackground(new IOSStyle.Glass(this, true, 24));
    controlStrip.setClipToOutline(true);
    TextView notifications = text("Thông báo", 12, Color.WHITE);
    notifications.setGravity(Gravity.CENTER);
    controlStrip.addView(notifications, new LinearLayout.LayoutParams(0, -1, 1));
    View divider = new View(this);
    divider.setBackgroundColor(0x40ffffff);
    controlStrip.addView(divider, new LinearLayout.LayoutParams(dp(1), dp(16)));
    TextView recent = text("Gần đây", 12, Color.WHITE);
    recent.setGravity(Gravity.CENTER);
    controlStrip.addView(recent, new LinearLayout.LayoutParams(0, -1, 1));
    controlStrip.setContentDescription(
        "Thanh điều khiển: chạm trái mở thông báo mới nhất, chạm phải mở ứng dụng gần đây, nhấn giữ"
            + " xem thông báo");
    controlParams =
        new WindowManager.LayoutParams(
            dp(176),
            dp(36),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
            android.graphics.PixelFormat.TRANSLUCENT);
    controlParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
    controlParams.setFitInsetsTypes(0);
    controlParams.layoutInDisplayCutoutMode =
        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
    controlParams.y = Math.max(statusBarBottom, dp(32)) + dp(Prefs.number(this, "control_gap", 4));
    try {
      wm.addView(controlStrip, controlParams);
      controlAttached = true;
      layoutControlStrip();
    } catch (RuntimeException ex) {
      Prefs.get(this)
          .edit()
          .putString("error", "Không tạo được thanh điều khiển: " + ex.getClass().getSimpleName())
          .apply();
    }
  }

  private void layoutControlStrip() {
    if (!controlAttached || controlStrip == null) return;
    int width = Math.min(screenWidth - dp(16), dp(Prefs.number(this, "control_width", 176))),
        height = dp(Prefs.number(this, "control_height", 36));
    int y = Math.max(statusBarBottom, dp(32)) + dp(Prefs.number(this, "control_gap", 4));
    boolean changed =
        controlParams.width != width || controlParams.height != height || controlParams.y != y;
    controlParams.width = width;
    controlParams.height = height;
    controlParams.y = y;
    controlStrip.headerHeight(height);
    boolean visible =
        Prefs.get(this).getBoolean("control_strip", true)
            && !expanded
            && !replyVisible
            && root.getVisibility() == View.VISIBLE
            && getSystemService(PowerManager.class).isInteractive();
    controlStrip.setVisibility(visible ? View.VISIBLE : View.GONE);
    if (changed)
      try {
        wm.updateViewLayout(controlStrip, controlParams);
      } catch (RuntimeException ex) {
        Prefs.get(this).edit().putString("error", "Thanh điều khiển cần khởi động lại").apply();
      }
  }

  private void updateLockSummary() {
    int count = Inbox.list(this).size();
    PendingIntent view =
        PendingIntent.getActivity(
            this,
            7,
            new Intent(this, UnlockActivity.class),
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    Notification notification =
        new Notification.Builder(this, "runtime")
            .setSmallIcon(R.drawable.ic_island)
            .setContentTitle("DynAmic Origin")
            .setContentText(count + " thông báo · Mở khóa để xem")
            .setContentIntent(view)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .build();
    getSystemService(NotificationManager.class).notify(17, notification);
  }

  private void requestUnlock() {
    try {
      startActivity(new Intent(this, UnlockActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    } catch (RuntimeException ex) {
      toast("Mở khóa điện thoại để xem nội dung");
    }
  }

  private void renderLocked(int available, int h, int idle, int count) {
    LinearLayout header = new LinearLayout(this);
    header.setGravity(Gravity.CENTER);
    header.setPadding(dp(6), 0, dp(6), 0);
    header.addView(text(count > 0 ? count + " thông báo" : "Đang chờ", 12, Color.WHITE));
    root.addView(header, new LinearLayout.LayoutParams(-1, h));
    if (expanded) {
      TextView message = text("Mở khóa để xem thông báo", 15, 0xffcccccc);
      message.setGravity(Gravity.CENTER);
      message.setPadding(dp(14), dp(12), dp(14), dp(12));
      root.addView(message);
      Button unlock = IOSStyle.button(this, "Mở khóa", true, true);
      unlock.setOnClickListener(v -> requestUnlock());
      LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(44));
      p.setMargins(dp(14), 0, dp(14), dp(14));
      root.addView(unlock, p);
    }
    resize(expanded ? Math.min(available, dp(280)) : idle, expanded ? h + dp(120) : h);
  }

  private void setIslandVisibility(int visibility) {
    root.setVisibility(visibility);
    if (overlayFrame != null) overlayFrame.setVisibility(visibility);
    layoutControlStrip();
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
    if (locked()) {
      requestUnlock();
      return;
    }
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
    updateDisplayGeometry();
    render();
  }

  @Override
  public void onDestroy() {
    if (displays != null) displays.unregisterDisplayListener(displayListener);
    recentWorker.shutdownNow();
    recentRequest++;
    handler.removeCallbacksAndMessages(null);
    if (animator != null) animator.cancel();
    if (controlAttached) {
      wm.removeView(controlStrip);
      controlAttached = false;
    }
    if (attached && root != null) wm.removeView(overlayFrame);
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
