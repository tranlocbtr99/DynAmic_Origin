package vn.loctm.dynamicorigin;

public class TransferPolicyTest {
  public static void main(String[] args) {
    for (int mask = 0; mask < 64; mask++) {
      boolean saved = (mask & 1) != 0,
          live = (mask & 2) != 0,
          ready = (mask & 4) != 0,
          enabled = (mask & 8) != 0,
          protectedItem = (mask & 16) != 0,
          locked = (mask & 32) != 0;
      boolean expected = mask == 15;
      if (TransferPolicy.allowed(saved, live, ready, enabled, protectedItem, locked) != expected)
        throw new AssertionError("Unsafe cancellation gate: " + mask);
    }
    System.out.println(
        "PASS: 64 transfer gate combinations; only durable live capture + visible overlay + opt-in"
            + " + unprotected + unlocked allows cancellation.");
  }
}
