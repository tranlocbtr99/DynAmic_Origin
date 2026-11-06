package vn.loctm.dynamicorigin;

final class IslandTap {
  static boolean right(float x, int width) {
    return width > 0 && x >= width / 2f;
  }
}
