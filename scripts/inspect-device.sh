#!/usr/bin/env bash
set -euo pipefail
serial="${1:?Usage: inspect-device.sh TV_LOCAL_IP:5555}"
[ "$(adb -s "$serial" get-state)" = device ] || exit 1
adb devices -l
for property in ro.product.model ro.product.device ro.build.version.release ro.build.version.sdk ro.build.display.id; do
  printf '%s: ' "$property"
  adb -s "$serial" shell getprop "$property"
done
adb -s "$serial" shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
