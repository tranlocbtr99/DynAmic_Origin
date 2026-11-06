package vn.loctm.dynamicorigin;

import android.content.Context;
import android.view.*;
import android.widget.LinearLayout;

/** Gesture owner survives child rebuilds and captures only the island header. */
final class IslandTouchLayout extends LinearLayout {
  interface Actions {
    void left();

    void right();

    void hold();

    void collapse();

    void finished();
  }

  private final GestureDetector detector;
  private final Actions actions;
  private boolean tracking;
  private int headerHeight, axisRotation;

  void axisRotation(int rotation) {
    axisRotation = rotation;
  }

  IslandTouchLayout(Context c, Actions a) {
    super(c);
    actions = a;
    setClickable(true);
    setFocusable(true);
    detector =
        new GestureDetector(
            c,
            new GestureDetector.SimpleOnGestureListener() {
              @Override
              public boolean onDown(MotionEvent e) {
                return true;
              }

              @Override
              public boolean onSingleTapUp(MotionEvent e) {
                if (IslandTap.rotatedRight(
                    e.getX(), e.getY(), getWidth(), getHeight(), axisRotation)) actions.right();
                else performClick();
                return true;
              }

              @Override
              public void onLongPress(MotionEvent e) {
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                actions.hold();
              }

              @Override
              public boolean onFling(MotionEvent start, MotionEvent end, float vx, float vy) {
                if (start != null && end.getY() < start.getY() - IOSStyle.dp(c, 12)) {
                  actions.collapse();
                  return true;
                }
                return false;
              }
            });
  }

  void headerHeight(int value) {
    headerHeight = value;
  }

  boolean touching() {
    return tracking;
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent e) {
    if (e.getActionMasked() == MotionEvent.ACTION_DOWN)
      tracking = e.getY() >= 0 && e.getY() < headerHeight;
    return tracking;
  }

  @Override
  public boolean onTouchEvent(MotionEvent e) {
    if (!tracking) return false;
    detector.onTouchEvent(e);
    if (e.getActionMasked() == MotionEvent.ACTION_UP
        || e.getActionMasked() == MotionEvent.ACTION_CANCEL) {
      tracking = false;
      post(actions::finished);
    }
    return true;
  }

  @Override
  public boolean performClick() {
    super.performClick();
    actions.left();
    return true;
  }
}
