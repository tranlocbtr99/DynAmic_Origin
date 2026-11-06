package vn.loctm.dynamicorigin;

final class IslandSizing {
  static int width(int screen, int percent, int available) {
    return Math.max(1, Math.min(available, Math.round(screen * percent / 100f)));
  }
}
