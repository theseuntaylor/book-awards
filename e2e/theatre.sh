#!/usr/bin/env bash
# E2E: the Theatre tab lists shows by section, hides finished runs, filters by venue, saves a status,
# schedules reminders for it, and opens the venue's page to book.
# Usage: e2e/theatre.sh [device-serial]   (defaults to the only connected device)
# Serves dated test shows from a local server through `adb reverse`, and clears the app's data first.
# Installs on that device only. Leaves screenshots, UI dumps, the served file, the request log, the
# scheduled-job dump and result.txt in e2e/artifacts/theatre/<timestamp>/.
# Reinstalls the normal build and clears the test data when it finishes.
set -euo pipefail

cd "$(dirname "$0")/.."
ADB="${ANDROID_HOME:?set ANDROID_HOME}/platform-tools/adb"
if [[ $# -ge 1 ]]; then export ANDROID_SERIAL="$1"; fi
# Gradle's installDebug honours ANDROID_SERIAL, so other connected devices are left alone.
adb() { "$ADB" "$@"; }

APP=com.theseuntaylor.bookawards
PORT=8766
OUT="e2e/artifacts/theatre/$(date +%Y%m%d-%H%M%S)"
SERVED="$OUT/served-shows.json"
REQUESTS="$OUT/requests.log"
mkdir -p "$OUT"
touch "$REQUESTS"

server_pid=""
cleanup() {
    if [[ -n "$server_pid" ]]; then kill "$server_pid" 2>/dev/null || true; wait "$server_pid" 2>/dev/null || true; fi
    adb reverse --remove "tcp:$PORT" >/dev/null 2>&1 || true
    ./gradlew :composeApp:installDebug -q >/dev/null 2>&1 || echo "warning: couldn't reinstall the normal build"
    adb shell pm clear "$APP" >/dev/null 2>&1 || true
}
trap cleanup EXIT

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
# Centre of the first node whose attributes match the regex $1, from the UI dump in $2.
center_in() {
    local bounds
    bounds=$(grep -oE "<node [^>]*$1[^>]*>" "$2" | head -1 | grep -oE 'bounds="\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]"' | grep -oE '[0-9]+' | tr '\n' ' ')
    [[ -n "$bounds" ]] || return 1
    read -r x1 y1 x2 y2 <<<"$bounds"
    echo "$(((x1 + x2) / 2)) $(((y1 + y2) / 2))"
}
top_of() { center_in "$1" "$2" | awk '{ print $2 }'; }
# Venue chips and list rows can carry the same text; the chip is the one highest on screen.
tap_topmost() {
    local file="$OUT/tap.xml"
    dump_ui >"$file"
    local point
    point=$(grep -oE "<node [^>]*$1[^>]*>" "$file" | grep -oE 'bounds="\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]"' | tr -c '0-9\n' ' ' |
        awk '{ print ($1 + $3) / 2, ($2 + $4) / 2 }' | sort -n -k2 | head -1)
    [[ -n "$point" ]] || fail "nothing on screen matches $1"
    read -r x y <<<"$point"
    adb shell input tap "${x%.*}" "${y%.*}"
}
tap() {
    local file="$OUT/tap.xml"
    dump_ui >"$file"
    local point
    point=$(center_in "$1" "$file") || fail "nothing on screen matches $1"
    read -r x y <<<"$point"
    adb shell input tap "$x" "$y"
}

# Test shows, dated from today so every run sees the same sections. "Opening soon" is a separate venue
# for the filter step; "Later" has both reminders still ahead of it.
python3 - >"$SERVED" <<'PY'
import datetime, json
today = datetime.date.today()
day = lambda n: (today + datetime.timedelta(days=n)).isoformat()
show = lambda slug, title, venue, start, end: {"id": "e2e:" + slug, "title": title, "venue": venue,
    "startDate": start, "endDate": end, "url": "https://example.org/e2e/" + slug, "note": "E2E note for " + title}
print(json.dumps({
    "generatedAt": datetime.datetime.now(datetime.timezone.utc).isoformat(timespec="milliseconds").replace("+00:00", "Z"),
    "shows": [
        show("ended", "E2E Ended", "E2E Playhouse", day(-30), day(-1)),
        show("on-now", "E2E On Now", "E2E Playhouse (Main House)", day(-5), day(20)),
        show("soon", "E2E Opening Soon", "E2E Studio", day(10), day(40)),
        show("later", "E2E Later", "E2E Playhouse", day(60), day(90)),
    ],
}))
PY

python3 e2e/serve_awards.py "$PORT" "$SERVED" "$REQUESTS" /shows.json & server_pid=$!
sleep 1
./gradlew :composeApp:installDebug -q -PshowsDataUrl="http://localhost:$PORT/shows.json"
adb reverse "tcp:$PORT" "tcp:$PORT" >/dev/null
adb shell pm clear "$APP" >/dev/null
# Granted up front so the bell doesn't stop at the system permission dialog.
adb shell pm grant "$APP" android.permission.POST_NOTIFICATIONS 2>/dev/null || true
adb shell am start -W -n "$APP/.MainActivity" >/dev/null

wait_for 'text="Theatre"' 15 || fail "the Theatre tab never appeared"
tap 'text="Theatre"'
wait_for 'text="E2E On Now"' 15 || { capture 0-no-test-data; fail "the served test shows never appeared (see requests.log)"; }
capture 1-sections
grep -q 'text="E2E Ended"' "$OUT/1-sections.xml" && fail "a show that ended yesterday is still listed"
order=("text=\"On now\"" 'text="E2E On Now"' 'text="Opening soon"' 'text="E2E Opening Soon"' 'text="Later"' 'text="E2E Later"')
previous=0
for item in "${order[@]}"; do
    y=$(top_of "$item" "$OUT/1-sections.xml") || fail "$item isn't on screen"
    (( y > previous )) || fail "$item is out of order"
    previous=$y
done

tap_topmost 'text="E2E Studio"'
sleep 1
capture 2-filtered
grep -q 'text="E2E Opening Soon"' "$OUT/2-filtered.xml" && fail "hiding E2E Studio left its show in the list"
grep -q 'text="E2E On Now"' "$OUT/2-filtered.xml" || fail "hiding E2E Studio also hid another venue's show"
tap_topmost 'text="E2E Studio"'
wait_for 'text="E2E Opening Soon"' 5 || fail "showing E2E Studio again didn't bring its show back"

tap 'text="E2E Later"'
wait_for 'content-desc="Back"' 5 || fail "tapping a show didn't open its screen"
wait_for 'text="E2E note for E2E Later"' 5 || fail "the show screen doesn't show why it's listed"
tap 'text="Want to see"'
sleep 1
capture 3-show-marked
tap 'content-desc="Back"'
wait_for 'text="Want to see"' 5 || { capture 4-list-no-chip; fail "the Want to see chip didn't show in the list"; }
capture 4-list-marked

tap 'content-desc="Remind me about shows I want to see"'
wait_for "You'll be reminded" 10 || { capture 5-no-reminder-message; fail "turning on show reminders didn't confirm"; }
capture 5-reminders-on
sleep 2
adb shell dumpsys jobscheduler >"$OUT/jobscheduler.txt"
jobs=$(grep -cE "^ *JOB .*@$APP/androidx.work" "$OUT/jobscheduler.txt" || true)
(( jobs >= 2 )) || fail "expected opening and closing reminders scheduled for E2E Later, found $jobs WorkManager jobs"

tap 'text="E2E Later"'
wait_for 'text="Book tickets"' 5 || fail "the show screen has no Book tickets button"
adb logcat -c
tap 'text="Book tickets"'
sleep 2
adb logcat -d -s ActivityTaskManager >"$OUT/book-tickets-logcat.txt"
grep -q 'act=android.intent.action.VIEW dat=https://example.org/' "$OUT/book-tickets-logcat.txt" ||
    fail "Book tickets didn't open the show's page"
capture 6-book-tickets

echo "PASS: sections in order, finished run hidden, venue filter, Want to see saved and shown, $jobs reminders scheduled, Book tickets opens the venue page" | tee "$OUT/result.txt"
