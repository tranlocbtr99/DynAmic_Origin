#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
policy_out="$(mktemp -d)"
trap 'rm -rf "$policy_out"' EXIT
javac -d "$policy_out" app/src/main/java/vn/loctm/dynamicorigin/TransferPolicy.java tests/TransferPolicyTest.java app/src/main/java/vn/loctm/dynamicorigin/UpdatePolicy.java tests/UpdatePolicyTest.java app/src/main/java/vn/loctm/dynamicorigin/IslandGeometry.java tests/IslandGeometryTest.java app/src/main/java/vn/loctm/dynamicorigin/RecentOrder.java app/src/main/java/vn/loctm/dynamicorigin/IslandTap.java tests/RecentOrderTest.java app/src/main/java/vn/loctm/dynamicorigin/IslandSizing.java tests/IslandSizingTest.java
java -cp "$policy_out" vn.loctm.dynamicorigin.TransferPolicyTest
java -cp "$policy_out" vn.loctm.dynamicorigin.UpdatePolicyTest
java -cp "$policy_out" vn.loctm.dynamicorigin.IslandGeometryTest
java -cp "$policy_out" vn.loctm.dynamicorigin.RecentOrderTest
java -cp "$policy_out" vn.loctm.dynamicorigin.IslandSizingTest
