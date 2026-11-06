package vn.loctm.dynamicorigin;

public class IslandSizingTest {
  public static void main(String[] args) {
    if (IslandSizing.width(1080, 24, 1048) != 259)
      throw new AssertionError("24% must not be inflated to 136/208dp");
    if (IslandSizing.width(1080, 8, 1048) != 86) throw new AssertionError("Small width");
    if (IslandSizing.width(1080, 90, 1048) != 972) throw new AssertionError("Wide width");
    if (IslandSizing.width(1080, 90, 500) != 500) throw new AssertionError("Screen bounds");
    System.out.println(
        "PASS: compact width respects exact percentage without notification-dependent minimums.");
  }
}
