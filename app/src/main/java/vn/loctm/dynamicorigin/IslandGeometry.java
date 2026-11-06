package vn.loctm.dynamicorigin;

/** Screen-space geometry only. Overlay-local insets must never enter this calculation. */
final class IslandGeometry {
  static int center(int screenWidth, int cameraCenter, int adjustment) {
    return Math.max(
        1,
        Math.min(
            screenWidth - 1, (cameraCenter >= 0 ? cameraCenter : screenWidth / 2) + adjustment));
  }

  static int left(int center, int width) {
    return center - width / 2;
  }

  static int maxWidth(int screenWidth, int center, int margin) {
    return Math.max(
        1,
        Math.min(screenWidth - margin * 2, 2 * (Math.min(center, screenWidth - center) - margin)));
  }
}
