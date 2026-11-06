package vn.loctm.dynamicorigin;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;

final class IOSToggle extends View {
  private boolean checked;
  private float position;
  private java.util.function.Consumer<Boolean> listener;
  private ValueAnimator animator;
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

  IOSToggle(Context c) {
    super(c);
    setClickable(true);
    setFocusable(true);
    setLayerType(LAYER_TYPE_SOFTWARE, null);
  }

  void setChecked(boolean value) {
    if (animator != null) animator.cancel();
    checked = value;
    position = value ? 1 : 0;
    invalidate();
  }

  boolean isChecked() {
    return checked;
  }

  void onChange(java.util.function.Consumer<Boolean> callback) {
    listener = callback;
  }

  @Override
  protected void onMeasure(int w, int h) {
    setMeasuredDimension(IOSStyle.dp(getContext(), 58), IOSStyle.dp(getContext(), 44));
  }

  @Override
  protected void onDraw(Canvas c) {
    float d = getResources().getDisplayMetrics().density;
    float x = (getWidth() - 51 * d) / 2, y = (getHeight() - 31 * d) / 2;
    paint.setColor(
        blend(IOSStyle.dark(getContext()) ? 0xff414148 : 0xffdcdce2, IOSStyle.GREEN, position));
    c.drawRoundRect(x, y, x + 51 * d, y + 31 * d, 16 * d, 16 * d, paint);
    paint.setColor(Color.WHITE);
    paint.setShadowLayer(2 * d, 0, 1 * d, 0x33000000);
    c.drawCircle(x + (15.5f + 20 * position) * d, y + 15.5f * d, 13.5f * d, paint);
    paint.clearShadowLayer();
  }

  private int blend(int a, int b, float f) {
    return Color.rgb(
        (int) (Color.red(a) + (Color.red(b) - Color.red(a)) * f),
        (int) (Color.green(a) + (Color.green(b) - Color.green(a)) * f),
        (int) (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * f));
  }

  @Override
  public boolean performClick() {
    super.performClick();
    checked = !checked;
    if (animator != null) animator.cancel();
    animator = ValueAnimator.ofFloat(position, checked ? 1 : 0);
    animator.setDuration(ValueAnimator.areAnimatorsEnabled() ? 180 : 0);
    animator.addUpdateListener(
        a -> {
          position = (float) a.getAnimatedValue();
          invalidate();
        });
    animator.start();
    if (listener != null) listener.accept(checked);
    return true;
  }

  @Override
  public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
    super.onInitializeAccessibilityNodeInfo(info);
    info.setClassName("android.widget.Switch");
    info.setCheckable(true);
    info.setChecked(checked);
  }

  @Override
  protected void onDetachedFromWindow() {
    if (animator != null) animator.cancel();
    super.onDetachedFromWindow();
  }
}
