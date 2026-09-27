#!/usr/bin/env bash
# E2E: build the awards dataset from live Wikidata + Wikipedia and check it has the lists the prizes have announced.
# Usage: e2e/pipeline.sh
# Doesn't touch data/awards.json. Leaves the generated file, the run log and result.txt in e2e/artifacts/pipeline/<timestamp>/.
set -euo pipefail

cd "$(dirname "$0")/.."
OUT="e2e/artifacts/pipeline/$(date +%Y%m%d-%H%M%S)"
mkdir -p "$OUT"

if ! node data/fetch-awards.mjs --out "$OUT/awards.json" >"$OUT/run.log" 2>&1; then
    echo "FAIL: the pipeline didn't finish; see $OUT/run.log" | tee "$OUT/result.txt"
    tail -5 "$OUT/run.log"
    exit 1
fi

node e2e/check-pipeline.mjs "$OUT/awards.json" data/awards.json | tee "$OUT/result.txt"
[[ "${PIPESTATUS[0]}" == 0 ]]
