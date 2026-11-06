package vn.loctm.dynamicorigin;

public class IslandGeometryTest {
  public static void main(String[] args) {
    int checks = 0;
    for (int screen : new int[] {720, 1080, 1260, 1440, 2400})
      for (int shift : new int[] {-120, 0, 120}) {
        int center = IslandGeometry.center(screen, screen / 2, shift);
        int max = IslandGeometry.maxWidth(screen, center, 16);
        for (int frame = 0; frame < 1000; frame++) {
          int width = 60 + (frame * 31) % (max - 59);
          int x = IslandGeometry.left(center, width);
          if (Math.abs(x + width / 2.0 - center) > 0.5) throw new AssertionError("Center drift");
          if (x < 0 || x + width > screen) throw new AssertionError("Off screen");
          checks++;
        }
      }
    if (IslandGeometry.center(1080, -1, 0) != 540)
      throw new AssertionError("Missing-cutout fallback");
    System.out.println(
        "PASS: " + checks + " animated sizes preserve screen anchor within half a pixel.");
  }
}
