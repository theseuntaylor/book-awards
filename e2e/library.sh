#!/usr/bin/env bash
# E2E: Home shows only the last three award years and links to the Library; the Library shows older years as
# shelves of covers that scroll sideways and open a book; the tabs and back navigation keep their places.
# Usage: e2e/library.sh [device-serial]   (defaults to the only connected device)
# Clears the app's data first. Leaves screenshots, UI dumps and result.txt in e2e/artifacts/library/<timestamp>/.
set -euo pipefail

cd "$(dirname "$0")/.."
ADB="${ANDROID_HOME:?set ANDROID_HOME}/platform-tools/adb"
SERIAL_ARGS=()
[[ $# -ge 1 ]] && SERIAL_ARGS=(-s "$1")
adb() { "$ADB" "${SERIAL_ARGS[@]}" "$@"; }

APP=com.theseuntaylor.bookawards
OUT="e2e/artifacts/library/$(date +%Y%m%d-%H%M%S)"
mkdir -p "$OUT"
FIRST_HOME_YEAR=$(( $(date +%Y) - 2 ))

dump_ui() { adb shell uiautomator dump /sdcard/e2e-ui.xml >/dev/null && adb shell cat /sdcard/e2e-ui.xml; }
fail() { echo "FAIL: $1" | tee "$OUT/result.txt"; exit 1; }
capture() { adb exec-out screencap -p >"$OUT/$1.png"; dump_ui >"$OUT/$1.xml"; }
wait_for() {
    for _ in $(seq 1 "$2"); do
        dump_ui | grep -qE "$1" && return 0
        sleep 1
    done
    return 1
}
years_on_screen() { grep -oE 'text="(19|20)[0-9]{2}"' | grep -oE '[0-9]{4}' || true; }
center_of() { tr '[],' '   ' <<<"$1" | awk '{ printf "%d %d", ($1 + $3) / 2, ($2 + $4) / 2 }'; }
bounds_of() { grep -o "$1[^>]*bounds=\"[^\"]*\"" | head -1 | grep -o '\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]'; }
tap_node() {
    local bounds
    bounds=$(dump_ui | bounds_of "$1")
    [[ -n "$bounds" ]] || fail "$2"
    read -r x y <<<"$(center_of "$bounds")"
    adb shell input tap "$x" "$y"
}

./gradlew :composeApp:installDebug -q
adb shell pm clear "$APP" >/dev/null
adb shell am start -W -n "$APP/.MainActivity" >/dev/null
wait_for 'text="Library"' 15 || fail "no Library tab in the navigation bar"
capture 1-home

# 1. Home: scroll to the end, recording every year header on the way; none may be older than the window.
seen_years=""
for _ in $(seq 1 25); do
    screen=$(dump_ui)
    seen_years+=" $(years_on_screen <<<"$screen" | tr '\n' ' ')"
    grep -q 'text="Earlier years are in the Library"' <<<"$screen" && break
    adb shell input swipe 540 1800 540 600 300
done
grep -q 'text="Earlier years are in the Library"' <<<"$(dump_ui)" || fail "Home never reached the Library link"
capture 2-home-end
for year in $seen_years; do
    (( year >= FIRST_HOME_YEAR )) || fail "Home shows $year, older than $FIRST_HOME_YEAR"
done

# 2. The link opens the Library, whose first shelf is the year before Home's window.
tap_node 'text="Earlier years are in the Library"' "no Library link to tap"
wait_for "text=\"$(( FIRST_HOME_YEAR - 1 ))\"" 10 || fail "the Library doesn't start with $(( FIRST_HOME_YEAR - 1 ))"
# A cover only gets its "Cover of …" description once the image has loaded, so this waits for real covers.
covers=0
for _ in $(seq 1 30); do
    covers=$(dump_ui | grep -o 'content-desc="Cover of' | wc -l | tr -d ' ')
    (( covers >= 4 )) && break
    sleep 1
done
capture 3-library
library_years=$(years_on_screen <"$OUT/3-library.xml" | tr '\n' ' ')
for year in $library_years; do
    (( year < FIRST_HOME_YEAR )) || fail "the Library shows $year, which belongs on Home"
done
covers=$(grep -o 'content-desc="Cover of' "$OUT/3-library.xml" | wc -l | tr -d ' ')
(( covers >= 4 )) || fail "only $covers covers loaded on the Library's first shelves within 30 seconds"

# 3. A shelf scrolls sideways.
first_card=$(bounds_of 'content-desc="Cover of' <"$OUT/3-library.xml")
read -r _ card_y <<<"$(center_of "$first_card")"
before=$(grep -o 'content-desc="Cover of[^"]*"' "$OUT/3-library.xml" | head -1)
adb shell input swipe 900 "$card_y" 150 "$card_y" 300
sleep 1
capture 4-shelf-scrolled
after=$(grep -o 'content-desc="Cover of[^"]*"' "$OUT/4-shelf-scrolled.xml" | head -1)
[[ "$before" != "$after" ]] || fail "the first shelf didn't scroll sideways"

# 4. A cover opens its book, and back returns to the Library.
tap_node 'content-desc="Cover of' "no cover to tap"
wait_for 'content-desc="Back"' 5 || fail "tapping a cover didn't open the book"
capture 5-book-from-library
adb shell input keyevent KEYCODE_BACK
wait_for "text=\"$(( FIRST_HOME_YEAR - 1 ))\"" 5 || fail "back didn't return to the Library"

# 5. The Home tab goes back to Home.
tap_node 'text="Home"' "no Home tab to tap"
wait_for 'text="Book Awards"' 5 || fail "the Home tab didn't open Home"
capture 6-home-again

echo "PASS: Home shows $FIRST_HOME_YEAR onwards and links to the Library; Library shelves start at $(( FIRST_HOME_YEAR - 1 )) with $covers covers on screen, scroll sideways and open books; tabs work" | tee "$OUT/result.txt"
