package vn.loctm.dynamicorigin;

/** Maps a handset-fixed natural-orientation rectangle into rotated display coordinates. */
final class PinnedFrame {
  static int[] map(
      int rotation, int naturalWidth, int naturalHeight, int x, int y, int width, int height) {
    return switch (rotation) {
      case 1 -> new int[] {y, naturalWidth - x - width, height, width};
      case 2 -> new int[] {naturalWidth - x - width, naturalHeight - y - height, width, height};
      case 3 -> new int[] {naturalHeight - y - height, x, height, width};
      default -> new int[] {x, y, width, height};
    };
  }

  static int cameraX(int rotation, int naturalWidth, int naturalHeight, int x, int y) {
    return switch (rotation) {
      case 1 -> naturalWidth - y;
      case 2 -> naturalWidth - x;
      case 3 -> y;
      default -> x;
    };
  }
}
