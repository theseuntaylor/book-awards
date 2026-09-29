#!/usr/bin/env bash
# E2E (iOS): runs the SwiftUI app's UI test (iosApp/iosAppUITests) on a simulator: Home's recent years and
# Library link, the Library's cover shelves, opening a book, setting a reading status, and the tabs.
# Usage: e2e/ios.sh [simulator-udid]   (defaults to the newest available iPhone simulator)
# Leaves the test result bundle, the exported screenshots, the build log and result.txt in e2e/artifacts/ios/<timestamp>/.
set -euo pipefail

cd "$(dirname "$0")/.."
OUT="e2e/artifacts/ios/$(date +%Y%m%d-%H%M%S)"
mkdir -p "$OUT"

udid="${1:-$(xcrun simctl list devices available | grep -E '^\s+iPhone' | tail -1 | grep -oE '[0-9A-F-]{36}')}"
[[ -n "$udid" ]] || { echo "FAIL: no iPhone simulator available" | tee "$OUT/result.txt"; exit 1; }

(cd iosApp && xcodegen generate --quiet)

status=0
xcodebuild test \
    -project iosApp/iosApp.xcodeproj \
    -scheme iosAppE2E \
    -destination "platform=iOS Simulator,id=$udid" \
    -resultBundlePath "$OUT/result.xcresult" >"$OUT/xcodebuild.log" 2>&1 || status=$?

xcrun xcresulttool export attachments --path "$OUT/result.xcresult" --output-path "$OUT/screenshots" >/dev/null 2>&1 || true
# Keep the test's named screenshots (e.g. 3-library.png), dropping XCTest's automatic UI snapshots.
python3 - "$OUT/screenshots" <<'EOF' || true
import json, os, re, sys
folder = sys.argv[1]
for test in json.load(open(os.path.join(folder, "manifest.json"))):
    for attachment in test.get("attachments", []):
        name, exported = attachment.get("suggestedHumanReadableName", ""), attachment["exportedFileName"]
        match = re.match(r"(\d+-[a-z-]+)_", name)
        path = os.path.join(folder, exported)
        if match: os.rename(path, os.path.join(folder, match.group(1) + ".png"))
        elif os.path.exists(path): os.remove(path)
EOF

if [[ $status -ne 0 ]]; then
    failure=$(grep -E "error: -\[|error:|XCTAssert|failed \(" "$OUT/xcodebuild.log" | head -3 || true)
    echo "FAIL: ${failure:-the UI test run failed; see $OUT/xcodebuild.log}" | tee "$OUT/result.txt"
    exit 1
fi
echo "PASS: Home shows recent years and links to the Library; covers load on sideways-scrolling shelves; a book opens, takes a reading status that shows on its shelf; tabs switch" | tee "$OUT/result.txt"
