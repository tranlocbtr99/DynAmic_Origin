#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
policy_out="$(mktemp -d)"
trap 'rm -rf "$policy_out"' EXIT
javac -d "$policy_out" app/src/main/java/vn/loctm/dynamicorigin/TransferPolicy.java tests/TransferPolicyTest.java
java -cp "$policy_out" vn.loctm.dynamicorigin.TransferPolicyTest
