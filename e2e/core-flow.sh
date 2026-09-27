#!/usr/bin/env bash
# E2E: open a book from the list, see its details load, mark it "Want to read", and see the chip back in the list.
# Usage: e2e/core-flow.sh [device-serial]   (defaults to the only connected device)
# Clears the app's data first so every run starts from the same state.
# Leaves screenshots, UI dumps and result.txt in e2e/artifacts/core-flow/<timestamp>/.
set -euo pipefail

cd "$(dirname "$0")/.."
ADB="${ANDROID_HOME:?set ANDROID_HOME}/platform-tools/adb"
SERIAL_ARGS=()
[[ $# -ge 1 ]] && SERIAL_ARGS=(-s "$1")
adb() { "$ADB" "${SERIAL_ARGS[@]}" "$@"; }

APP=com.theseuntaylor.bookawards
OUT="e2e/artifacts/core-flow/$(date +%Y%m%d-%H%M%S)"
mkdir -p "$OUT"

dump_ui() { adb shell uiautomator dump /sdcard/e2e-ui.xml >/dev/null && adb shell cat /sdcard/e2e-ui.xml; }
fail() { echo "FAIL: $1" | tee "$OUT/result.txt"; exit 1; }
capture() { adb exec-out screencap -p >"$OUT/$1.png"; dump_ui >"$OUT/$1.xml"; }
center_of() { tr '[],' '   ' <<<"$1" | awk '{ printf "%d %d", ($1 + $3) / 2, ($2 + $4) / 2 }'; }

# Waits up to $2 seconds for the UI to contain the regex $1.
wait_for() {
    for _ in $(seq 1 "$2"); do
        dump_ui | grep -qE "$1" && return 0
        sleep 1
    done
    return 1
}

./gradlew :composeApp:installDebug -q
adb shell pm clear "$APP" >/dev/null
adb shell am start -W -n "$APP/.MainActivity" >/dev/null

wait_for 'text="20[0-9][0-9]"' 15 || fail "the list never showed a year"
capture 1-list

# The first tappable row below the topmost year header is the newest book.
# (uiautomator doesn't list sticky headers in screen order, so sort by position.)
list=$(cat "$OUT/1-list.xml")
header_bottom=$(grep -o 'text="20[0-9][0-9]"[^>]*bounds="[^"]*"' <<<"$list" | grep -o '\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]' |
    tr '[],' '   ' | sort -n -k2 | head -1 | awk '{ print $4 }')
row=$(grep -o 'clickable="true"[^>]*bounds="[^"]*"' <<<"$list" | grep -o '\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]' |
    awk -v min="$header_bottom" -F'[][,]' '$3 >= min { print; exit }')
[[ -n "$row" ]] || fail "no book row below the first year header"
read -r x y <<<"$(center_of "$row")"
adb shell input tap "$x" "$y"

wait_for 'content-desc="Back"' 5 || fail "tapping the first book didn't open its detail screen"
grep -q 'text="Nominations"' <<<"$(dump_ui)" || fail "the detail screen has no Nominations section"
# A long description can push the result below the fold, so wait for the loading text to go instead.
lookup_done=0
for _ in $(seq 1 20); do
    dump_ui | grep -q 'Looking this book up on Open Library' || { lookup_done=1; break; }
    sleep 1
done
[[ $lookup_done == 1 ]] || fail "the Open Library lookup never finished"
capture 2-detail

want=$(grep -o 'text="Want to read"[^>]*bounds="[^"]*"' <<<"$(dump_ui)" | grep -o '\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]' | head -1)
[[ -n "$want" ]] || fail "no Want to read button"
read -r x y <<<"$(center_of "$want")"
adb shell input tap "$x" "$y"
sleep 1
adb shell input keyevent KEYCODE_BACK

wait_for 'text="Book Awards"' 5 || fail "back didn't return to the list"
capture 3-list-with-chip
# The detail screen's button has the same label, so only count it once we're definitely on the list.
grep -q 'content-desc="Back"' "$OUT/3-list-with-chip.xml" && fail "still on the detail screen after pressing back"
grep -q 'text="Want to read"' "$OUT/3-list-with-chip.xml" || fail "the list shows no Want to read chip after marking the book"

echo "PASS: opened the first book, its details loaded, and the Want to read chip showed in the list" | tee "$OUT/result.txt"
