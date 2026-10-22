#!/usr/bin/env bash
# E2E: build the theatre listings from the live venue pages and check what would be published.
# Usage: e2e/shows-pipeline.sh
# Doesn't touch data/shows.json. Leaves the generated file, the candidates list, the run log and result.txt
# in e2e/artifacts/shows-pipeline/<timestamp>/.
set -euo pipefail

cd "$(dirname "$0")/.."
OUT="e2e/artifacts/shows-pipeline/$(date +%Y%m%d-%H%M%S)"
mkdir -p "$OUT"
TODAY=$(TZ=Europe/London date +%Y-%m-%d)

node --test data/theatre/ >"$OUT/unit-tests.log" 2>&1 || {
    echo "FAIL: the date and curation unit tests failed; see $OUT/unit-tests.log" | tee "$OUT/result.txt"
    exit 1
}

if ! node data/fetch-shows.mjs --out "$OUT/shows.json" --candidates "$OUT/show-candidates.md" --today "$TODAY" >"$OUT/run.log" 2>&1; then
    echo "FAIL: the pipeline refused to write; see $OUT/run.log" | tee "$OUT/result.txt"
    tail -8 "$OUT/run.log"
    exit 1
fi

node e2e/check-shows.mjs "$OUT/shows.json" "$OUT/run.log" "$TODAY" | tee "$OUT/result.txt"
[[ "${PIPESTATUS[0]}" == 0 ]]
