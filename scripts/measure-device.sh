#!/usr/bin/env bash
set -euo pipefail
serial="${1:?Usage: measure-device.sh TV_LOCAL_IP:5555}"
[ "$(adb -s "$serial" get-state)" = device ] || exit 1
for run in 1 2 3; do
  printf '\nCold launch %s\n' "$run"
  adb -s "$serial" shell am force-stop com.zklabs.tvos
  adb -s "$serial" shell am start -W -n com.zklabs.tvos/.MainActivity
done
adb -s "$serial" shell dumpsys meminfo com.zklabs.tvos
adb -s "$serial" shell dumpsys gfxinfo com.zklabs.tvos
