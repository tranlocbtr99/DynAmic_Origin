package vn.loctm.dynamicorigin;

import java.util.Objects;

final class UpdatePolicy {
  static boolean shouldExpand(
      boolean hasPrevious,
      boolean ongoing,
      String title,
      String text,
      String previousTitle,
      String previousText) {
    return !hasPrevious
        || (!ongoing
            && (!Objects.equals(title, previousTitle) || !Objects.equals(text, previousText)));
  }
}
