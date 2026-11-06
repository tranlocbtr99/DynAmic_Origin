package vn.loctm.dynamicorigin;

import java.util.Arrays;

public class PinnedFrameTest {
  public static void main(String[] args) {
    int count = 0;
    for (int width : new int[] {86, 259, 400, 972})
      for (int y : new int[] {0, 24, 200})
        for (int rotation = 0; rotation < 4; rotation++) {
          int x = 540 - width / 2, h = 80;
          int[] mapped = PinnedFrame.map(rotation, 1080, 2400, x, y, width, h);
          int rw = (rotation % 2 == 0) ? 1080 : 2400, rh = (rotation % 2 == 0) ? 2400 : 1080;
          int[] original =
              PinnedFrame.map(
                  (4 - rotation) % 4, rw, rh, mapped[0], mapped[1], mapped[2], mapped[3]);
          if (!Arrays.equals(original, new int[] {x, y, width, h}))
            throw new AssertionError("Physical rectangle drift");
          count++;
        }
    if (PinnedFrame.cameraX(1, 1080, 2400, 30, 540) != 540
        || PinnedFrame.cameraX(3, 1080, 2400, 2370, 540) != 540)
      throw new AssertionError("Camera normalization");
    System.out.println(
        "PASS: "
            + count
            + " rotated frames retain identical handset-fixed position and dimensions.");
  }
}
