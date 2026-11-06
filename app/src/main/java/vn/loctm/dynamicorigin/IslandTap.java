package vn.loctm.dynamicorigin;

final class IslandTap {
  static boolean right(float x, int width) {
    return width > 0 && x >= width / 2f;
  }

  static boolean rotatedRight(float x, float y, int width, int height, int rotation) {
    return switch (rotation) {
      case 1 -> right(height - y, height);
      case 2 -> right(width - x, width);
      case 3 -> right(y, height);
      default -> right(x, width);
    };
  }
}
