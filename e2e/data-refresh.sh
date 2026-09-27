#!/usr/bin/env bash
# E2E: the app picks up newer awards data from its data URL, keeps it offline, and ignores bad publishes.
# Usage: e2e/data-refresh.sh [device-serial]   (defaults to the only connected device)
# Serves test data from a local server through `adb reverse`, and clears the app's data first.
# Leaves screenshots, UI dumps, the server's request log and result.txt in e2e/artifacts/data-refresh/<timestamp>/.
# Reinstalls the normal build (published data URL) and clears the test data when it finishes.
set -euo pipefail

cd "$(dirname "$0")/.."
ADB="${ANDROID_HOME:?set ANDROID_HOME}/platform-tools/adb"
SERIAL_ARGS=()
[[ $# -ge 1 ]] && SERIAL_ARGS=(-s "$1")
adb() { "$ADB" "${SERIAL_ARGS[@]}" "$@"; }

APP=com.theseuntaylor.bookawards
PORT=8765
OUT="e2e/artifacts/data-refresh/$(date +%Y%m%d-%H%M%S)"
SERVED="$OUT/served-awards.json"
REQUESTS="$OUT/requests.log"
mkdir -p "$OUT"
touch "$REQUESTS"

server_pid=""
start_server() { python3 e2e/serve_awards.py "$PORT" "$SERVED" "$REQUESTS" & server_pid=$!; sleep 1; }
# `wait` reports the killed server's exit status (143), which isn't a failure here.
stop_server() {
    if [[ -n "$server_pid" ]]; then
        kill "$server_pid" 2>/dev/null || true
        wait "$server_pid" 2>/dev/null || true
    fi
    server_pid=""
}
cleanup() {
    stop_server || true
    adb reverse --remove "tcp:$PORT" >/dev/null 2>&1 || true
    ./gradlew :composeApp:installDebug -q >/dev/null 2>&1 || echo "warning: couldn't reinstall the normal build"
    # Otherwise the test data stays cached and keeps showing in the normal build.
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
# The next step's message may be identical to the last one, so wait until no snackbar is showing.
wait_for_snackbar_to_clear() {
    for _ in $(seq 1 15); do
        dump_ui | grep -qE 'new nomination|check for new nominations|up to date' || return 0
        sleep 1
    done
    fail "a snackbar never cleared"
}
launch() { adb shell am start -S -W -n "$APP/.MainActivity" >/dev/null; }
pull_to_refresh() { adb shell input swipe 540 700 540 1700 600; }

# Test data: the real file plus extra entries. Year 2099 puts them at the top of the list.
# Dated now, so it's newer than the bundled data; GENERATED_AT overrides that (e.g. to simulate a stale CDN copy).
with_entries() {
    GENERATED_AT="${GENERATED_AT:-}" python3 - "$@" >"$SERVED" <<'EOF'
import datetime, json, os, sys
data = json.load(open("data/awards.json"))
now = datetime.datetime.now(datetime.timezone.utc).isoformat(timespec="milliseconds").replace("+00:00", "Z")
data["generatedAt"] = os.environ["GENERATED_AT"] or now
for spec in sys.argv[1:]:
    award, title = spec.split(":", 1)
    data["nominations"].append({"award": award, "year": 2099, "title": title, "author": "E2E Author",
                                "status": "WINNER", "wikidataId": "E2E-" + title.replace(" ", "-")})
json.dump(data, sys.stdout)
EOF
}

./gradlew :composeApp:installDebug -q -PawardsDataUrl="http://localhost:$PORT/awards.json"
adb reverse "tcp:$PORT" "tcp:$PORT" >/dev/null
adb shell pm clear "$APP" >/dev/null

# 1. Newer data is fetched on launch and announced.
with_entries "BOOKER:E2E Test Novel"
start_server
launch
wait_for 'text="E2E Test Novel"' 20 || fail "1: the newer data never appeared"
wait_for 'text="1 new nomination"' 5 || fail "1: no '1 new nomination' snackbar"
capture 1-new-data
head -1 "$REQUESTS" | grep -q 'status=200 if-none-match=-' || fail "1: first request wasn't a plain 200"

# 2. Relaunching sends the ETag and gets 304; the cached data is still shown.
launch
wait_for 'text="E2E Test Novel"' 15 || fail "2: cached data missing after relaunch"
sleep 2
tail -1 "$REQUESTS" | grep -qE 'status=304 if-none-match="[0-9a-f]+"' || fail "2: relaunch didn't get a 304 with If-None-Match"
capture 2-not-modified

# 3. Offline: no server, the cached data still loads.
stop_server
launch
wait_for 'text="E2E Test Novel"' 15 || fail "3: cached data missing while offline"
capture 3-offline

# 4. A broken publish (e.g. a captive-portal page) is rejected on pull to refresh.
echo '<html>Log in to Wi-Fi</html>' >"$SERVED"
start_server
pull_to_refresh
wait_for 'text="Couldn(&apos;|.)t check for new nominations"' 15 || fail "4: no failure snackbar for malformed data"
dump_ui | grep -q 'text="E2E Test Novel"' || fail "4: malformed data replaced the good data"
capture 4-malformed-rejected

# 5. A suspiciously small publish is rejected too.
with_entries
python3 -c 'import json, sys; d = json.load(open(sys.argv[1])); d["nominations"] = d["nominations"][:5]; print(json.dumps(d))' "$SERVED" >"$SERVED.tmp"
mv "$SERVED.tmp" "$SERVED"
wait_for_snackbar_to_clear
pull_to_refresh
wait_for 'text="Couldn(&apos;|.)t check for new nominations"' 15 || fail "5: no failure snackbar for a truncated publish"
dump_ui | grep -q 'text="E2E Test Novel"' || fail "5: a truncated publish replaced the good data"
capture 5-truncated-rejected

# 6. An award this app doesn't know is skipped; the rest of the update still lands.
with_entries "BOOKER:E2E Test Novel" "WOMENS_PRIZE:E2E Unknown Award Novel" "BOOKER:E2E Second Novel"
wait_for_snackbar_to_clear
pull_to_refresh
wait_for 'text="E2E Second Novel"' 15 || fail "6: the update with an unknown award was rejected entirely"
dump_ui | grep -q 'E2E Unknown Award Novel' && fail "6: the unknown-award entry was shown"
capture 6-unknown-award-skipped

# 7. An older file (a stale CDN copy, or data older than a new app's bundle) never replaces newer data.
GENERATED_AT="2000-01-01T00:00:00.000Z" with_entries "BOOKER:E2E Stale Novel"
wait_for_snackbar_to_clear
pull_to_refresh
wait_for "text=\"You(&apos;|.)re up to date\"" 15 || fail "7: no 'up to date' snackbar for an older file"
dump_ui | grep -q 'E2E Stale Novel' && fail "7: an older file replaced newer data"
dump_ui | grep -q 'text="E2E Second Novel"' || fail "7: the newer data disappeared"
capture 7-older-file-ignored

echo "PASS: new data arrives with ETag/304 caching, survives offline, and malformed, truncated, unknown-award and older publishes are handled" | tee "$OUT/result.txt"
