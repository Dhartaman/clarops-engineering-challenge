#!/usr/bin/env bash

set -euo pipefail

RUN_ID="${RUN_ID:-$(date +%Y%m%d%H%M%S)-$RANDOM}"

printf 'Using RUN_ID=%s\n' "$RUN_ID"
hurl --test --variable runId="$RUN_ID" hurl/*.hurl
