package vn.loctm.dynamicorigin;

import android.app.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;

/** Shared geometry and material hierarchy: opaque content, glass controls, black island. */
final class IOSStyle {
  static final int BLUE = 0xff007aff, GREEN = 0xff34c759;

  static int dp(Context c, float v) {
    return Math.round(v * c.getResources().getDisplayMetrics().density);
  }

  static boolean dark(Context c) {
    return (c.getResources().getConfiguration().uiMode & 0x30) == 0x20;
  }

  static int ink(Context c) {
    return dark(c) ? 0xfff5f5f7 : 0xff17171c;
  }

  static int secondary(Context c) {
    return dark(c) ? 0xffa5a5af : 0xff6d6d78;
  }

  static GradientDrawable surface(Context c, boolean dark, float radius) {
    GradientDrawable d = new GradientDrawable();
    d.setColor(dark ? 0xff17171c : 0xeefdfdff);
    d.setCornerRadius(dp(c, radius));
    d.setStroke(dp(c, 0.5f), dark ? 0x1affffff : 0x20000000);
    return d;
  }

  static void text(TextView v, int size, boolean medium, int color) {
    v.setTextSize(size);
    v.setTextColor(color);
    v.setFontFeatureSettings("kern");
    v.setTypeface(Typeface.create(medium ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
    v.setIncludeFontPadding(false);
  }

  static TextView text(Context c, String value, int size, boolean medium, int color) {
    TextView v = new TextView(c);
    v.setText(value);
    text(v, size, medium, color);
    return v;
  }

  static LinearLayout column(Context c) {
    LinearLayout l = new LinearLayout(c);
    l.setOrientation(LinearLayout.VERTICAL);
    return l;
  }

  static LinearLayout card(Context c) {
    LinearLayout l = column(c);
    l.setPadding(dp(c, 18), dp(c, 12), dp(c, 18), dp(c, 12));
    l.setBackground(surface(c, dark(c), 24));
    return l;
  }

  static Button button(Context c, String label, boolean primary, boolean onBlack) {
    Button b = new Button(c);
    b.setText(label);
    b.setAllCaps(false);
    text(b, 15, true, primary ? Color.WHITE : onBlack ? Color.WHITE : BLUE);
    b.setMinHeight(0);
    b.setMinimumHeight(0);
    b.setMinWidth(0);
    b.setMinimumWidth(0);
    b.setPadding(dp(c, 16), 0, dp(c, 16), 0);
    b.setStateListAnimator(null);
    Drawable base = primary ? solid(c, BLUE, 26) : new Glass(c, onBlack || dark(c), 26);
    b.setBackground(
        new RippleDrawable(ColorStateList.valueOf(onBlack ? 0x33ffffff : 0x22007aff), base, null));
    b.setOnTouchListener(
        (v, e) -> {
          if (e.getActionMasked() == android.view.MotionEvent.ACTION_DOWN)
            v.animate().scaleX(.97f).scaleY(.97f).setDuration(110).start();
          if (e.getActionMasked() == android.view.MotionEvent.ACTION_UP
              || e.getActionMasked() == android.view.MotionEvent.ACTION_CANCEL)
            v.animate().scaleX(1).scaleY(1).setDuration(160).start();
          return false;
        });
    return b;
  }

  static GradientDrawable solid(Context c, int color, float radius) {
    GradientDrawable d = new GradientDrawable();
    d.setColor(color);
    d.setCornerRadius(dp(c, radius));
    return d;
  }

  static void page(Activity a) {
    a.getWindow().setDecorFitsSystemWindows(false);
    a.getWindow().setStatusBarColor(Color.TRANSPARENT);
    a.getWindow().setNavigationBarColor(Color.TRANSPARENT);
    WindowInsetsController controller = a.getWindow().getInsetsController();
    if (controller != null)
      controller.setSystemBarsAppearance(
          dark(a)
              ? 0
              : WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                  | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
          WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
              | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
  }

  static void dialog(Dialog d) {
    materialWindow(d.getContext(), d.getWindow());
  }

  static void dialog(Activity a) {
    materialWindow(a, a.getWindow());
  }

  private static void materialWindow(Context c, Window w) {
    if (w == null) return;
    boolean blur = c.getSystemService(WindowManager.class).isCrossWindowBlurEnabled();
    GradientDrawable material = surface(c, dark(c), 28);
    material.setAlpha(blur ? 210 : 255);
    w.setBackgroundDrawable(material);
    w.setBackgroundBlurRadius(blur ? dp(c, 24) : 0);
    w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    WindowManager.LayoutParams p = w.getAttributes();
    p.dimAmount = .25f;
    w.setAttributes(p);
  }

  /** Tint and specular rim only; does not claim cross-window refraction. */
  static final class Glass extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final boolean dark;
    private final float radius;
    private int alpha = 255;

    Glass(Context c, boolean dark, float radius) {
      this.dark = dark;
      this.radius = dp(c, radius);
    }

    @Override
    public void draw(Canvas canvas) {
      RectF r = new RectF(getBounds());
      r.inset(.75f, .75f);
      paint.setAlpha(alpha);
      paint.setStyle(Paint.Style.FILL);
      paint.setShader(
          new LinearGradient(
              r.left,
              r.top,
              r.right,
              r.bottom,
              dark
                  ? new int[] {0xff35353c, 0xff25252c, 0xff1b1b21}
                  : new int[] {0xfffdfdff, 0xffe9edf7, 0xfff7f8fc},
              null,
              Shader.TileMode.CLAMP));
      canvas.drawRoundRect(r, radius, radius, paint);
      paint.setStyle(Paint.Style.STROKE);
      paint.setStrokeWidth(1.5f);
      paint.setShader(
          new LinearGradient(
              r.left,
              r.top,
              r.right,
              r.bottom,
              new int[] {
                dark ? 0x70ffffff : 0xe0ffffff,
                dark ? 0x08ffffff : 0x30616b82,
                dark ? 0x35ffffff : 0xaaffffff
              },
              null,
              Shader.TileMode.CLAMP));
      canvas.drawRoundRect(r, radius, radius, paint);
      paint.setShader(null);
      paint.setStyle(Paint.Style.FILL);
    }

    @Override
    public void setAlpha(int value) {
      alpha = value;
      invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter filter) {
      paint.setColorFilter(filter);
    }

    @Override
    public int getOpacity() {
      return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void getOutline(Outline outline) {
      outline.setRoundRect(getBounds(), radius);
    }
  }
}
