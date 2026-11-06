package vn.loctm.dynamicorigin;

import java.util.*;

final class RecentOrder {
  private final Map<String, Long> timestamps = new HashMap<>();

  void record(String pkg, long time) {
    if (pkg != null && !pkg.isBlank()) timestamps.merge(pkg, time, Math::max);
  }

  List<String> newest(int limit) {
    List<String> names = new ArrayList<>(timestamps.keySet());
    names.sort(
        Comparator.<String>comparingLong(timestamps::get)
            .reversed()
            .thenComparing(Comparator.naturalOrder()));
    return names.subList(0, Math.min(Math.max(0, limit), names.size()));
  }
}
