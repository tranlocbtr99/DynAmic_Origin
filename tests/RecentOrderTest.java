package vn.loctm.dynamicorigin;

import java.util.*;

public class RecentOrderTest {
  public static void main(String[] args) {
    RecentOrder order = new RecentOrder();
    order.record("chat", 100);
    order.record("bank", 300);
    order.record("chat", 400);
    order.record("chat", 200);
    order.record(null, 999);
    order.record("", 999);
    if (!order.newest(12).equals(Arrays.asList("chat", "bank")))
      throw new AssertionError("Repeated app not ranked by last resume");
    if (!order.newest(1).equals(Collections.singletonList("chat")))
      throw new AssertionError("Limit");
    if (!order.newest(0).isEmpty()) throw new AssertionError("Zero limit");
    for (int w : new int[] {120, 240, 400}) {
      if (IslandTap.right(0, w)
          || IslandTap.right(w / 2f - .1f, w)
          || !IslandTap.right(w / 2f, w)
          || !IslandTap.right(w - 1, w)) throw new AssertionError("Physical left/right routing");
    }
    if (IslandTap.rotatedRight(5, 150, 24, 200, 1)
        || !IslandTap.rotatedRight(5, 50, 24, 200, 1)
        || IslandTap.rotatedRight(5, 50, 24, 200, 3)
        || !IslandTap.rotatedRight(5, 150, 24, 200, 3))
      throw new AssertionError("Rotated extension hit zones");
    System.out.println(
        "PASS: recent app deduplication/ordering/limit and left/right hit boundaries.");
  }
}
