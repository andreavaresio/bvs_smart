#!/usr/bin/env bash
set -euo pipefail

API_ENDPOINT="${API_ENDPOINT:-http://localhost:9988/api.php}"
API_KEY="${API_KEY:-asdfjdsl567567sadfsda}"

if [[ $# -gt 1 ]]; then
  echo "Usage: $0 [json-payload-file]" >&2
  exit 64
fi

if [[ $# -eq 1 ]]; then
  JSON_PAYLOAD=$(<"$1")
else
  JSON_PAYLOAD='{"deviceid":"iPhone","status":"ok","firmware":"1.0.0","battery":0.87}'
fi

curl \
  --silent \
  --show-error \
  --request POST \
  --header 'Content-Type: application/json' \
  --data "$JSON_PAYLOAD" \
  "$API_ENDPOINT?api-key=$API_KEY"
