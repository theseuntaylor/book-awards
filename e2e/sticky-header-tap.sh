#!/usr/bin/env bash
# E2E: tapping a pinned year header must not open the book scrolling underneath it.
# Usage: e2e/sticky-header-tap.sh [device-serial]   (defaults to the only connected device)
# Leaves a screenshot, UI dump and result.txt in e2e/artifacts/sticky-header-tap/<timestamp>/.
set -euo pipefail

cd "$(dirname "$0")/.."
ADB="${ANDROID_HOME:?set ANDROID_HOME}/platform-tools/adb"
SERIAL_ARGS=()
[[ $# -ge 1 ]] && SERIAL_ARGS=(-s "$1")
adb() { "$ADB" "${SERIAL_ARGS[@]}" "$@"; }

OUT="e2e/artifacts/sticky-header-tap/$(date +%Y%m%d-%H%M%S)"
mkdir -p "$OUT"

dump_ui() { adb shell uiautomator dump /sdcard/e2e-ui.xml >/dev/null && adb shell cat /sdcard/e2e-ui.xml; }

./gradlew :composeApp:installDebug -q
adb shell am start -S -W -n com.theseuntaylor.bookawards/.MainActivity >/dev/null
sleep 3

# Scroll far enough that the first year header is pinned with a book row underneath it.
adb shell input swipe 540 1900 540 1300 400
sleep 1.5

before=$(dump_ui)
echo "$before" >"$OUT/before-tap.xml"
header_bounds=$(echo "$before" | grep -o 'text="20[0-9][0-9]"[^>]*bounds="\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]"' | head -1 | grep -o '\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]')
if [[ -z "$header_bounds" ]]; then
    echo "FAIL: no year header on screen" | tee "$OUT/result.txt"
    exit 1
fi
read -r left top right bottom <<<"$(echo "$header_bounds" | tr '[],' '   ')"
tap_x=$(( (left + right) / 2 ))
tap_y=$(( (top + bottom) / 2 ))

# Precondition: a tappable book row must sit under the header, or the test proves nothing.
row_under_header=$(echo "$before" | grep -o 'clickable="true"[^>]*bounds="[^"]*"' | grep -o '\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]' |
    tr '[],' '   ' | awk -v x="$tap_x" -v y="$tap_y" '$1 <= x && x <= $3 && $2 <= y && y <= $4 { found = 1 } END { print found + 0 }')
if [[ "$row_under_header" != 1 ]]; then
    echo "FAIL: setup — no book row under the pinned header at ($tap_x, $tap_y); adjust the scroll" | tee "$OUT/result.txt"
    exit 1
fi

adb shell input tap "$tap_x" "$tap_y"
sleep 1.5

adb exec-out screencap -p >"$OUT/after-tap.png"
dump_ui >"$OUT/after-tap.xml"

if grep -q 'content-desc="Back"' "$OUT/after-tap.xml"; then
    echo "FAIL: tapping the pinned header at ($tap_x, $tap_y) opened a book" | tee "$OUT/result.txt"
    adb shell input keyevent KEYCODE_BACK
    exit 1
fi
echo "PASS: tapping the pinned header at ($tap_x, $tap_y) left the list open" | tee "$OUT/result.txt"
