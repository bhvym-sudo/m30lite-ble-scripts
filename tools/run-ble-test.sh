#!/usr/bin/env bash
set -euo pipefail
repo_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
adb_cmd=(adb -s "${M30_SERIAL:-32001b7f4fda360d}")
mode="${1:-ordinary}"
duration="${2:-10}"
if [[ "$mode" != ordinary && ! "$mode" =~ ^[0-9a-fA-F]{6}$ ]]; then
  echo 'Usage: run-ble-test.sh ordinary|SIX_HEX_DIGIT_MODEL_ID [1..60 seconds]' >&2
  exit 2
fi
if [[ ! "$duration" =~ ^[0-9]{1,2}$ ]] || ((10#$duration < 1 || 10#$duration > 60)); then
  echo 'Duration must be 1..60 seconds' >&2
  exit 2
fi
"${adb_cmd[@]}" get-state >/dev/null
initial_bt="$("${adb_cmd[@]}" shell settings get global bluetooth_on | tr -d '\r')"
cleanup() {
  if [[ "$initial_bt" == 0 ]]; then
    "${adb_cmd[@]}" shell "su -c 'CLASSPATH=/data/local/tmp/m30-ble-probe.jar app_process /system/bin BleProbe off'" || true
  fi
}
trap cleanup EXIT
"${adb_cmd[@]}" push "$repo_dir/.build/ble/ble-probe.jar" /data/local/tmp/m30-ble-probe.jar
"${adb_cmd[@]}" shell "su -c 'CLASSPATH=/data/local/tmp/m30-ble-probe.jar app_process /system/bin BleProbe $mode $duration'"
