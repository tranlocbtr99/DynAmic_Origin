package vn.loctm.dynamicorigin;

final class TransferPolicy {
  static boolean allowed(
      boolean saved,
      boolean liveEvent,
      boolean overlayReady,
      boolean enabled,
      boolean protectedItem,
      boolean locked) {
    return saved && liveEvent && overlayReady && enabled && !protectedItem && !locked;
  }
}
