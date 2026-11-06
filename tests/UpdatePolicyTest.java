package vn.loctm.dynamicorigin;

public class UpdatePolicyTest {
  public static void main(String[] args) {
    check(true, false, false, "Hello", "New", null, null);
    check(true, false, true, "Track", "Artist", null, null);
    check(false, true, true, "Track 2", "New artist", "Track 1", "Old artist");
    check(false, true, false, "Hello", "Same", "Hello", "Same");
    check(true, true, false, "Hello", "New message", "Hello", "Old message");
    check(true, true, false, "New sender", "Message", "Old sender", "Message");
    System.out.println(
        "PASS: new activity/message opens; duplicate and ongoing updates stay compact.");
  }

  static void check(
      boolean expected,
      boolean old,
      boolean ongoing,
      String title,
      String text,
      String oldTitle,
      String oldText) {
    if (UpdatePolicy.shouldExpand(old, ongoing, title, text, oldTitle, oldText) != expected)
      throw new AssertionError("Unexpected expansion decision");
  }
}
