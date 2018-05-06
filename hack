#!/usr/bin/env bash

readonly host="${HOST:-localhost}"
readonly port="${PORT:-5432}"

add() {
  curl "$host:$port/api/device/$1" -XPOST
  echo ""
}

health() {
  curl "$host:$port/api/health"
  echo ""
}

# take coords in lat lon format
move() {
  export ID="$1"
  export POS="$2:$3"
  export EVENT="${EVENT:-geo}"

  jq -n '{coreid: env.ID, event: env.EVENT, published_at: "now", data: env.POS}' \
  | curl "$host:$port/api/device/$ID/move" -H 'content-type: application/json' -d @-

  echo ""
}

watch() {
  export WS_URI="http://$host:$port/api/watch"
  "$@"
}

device() {
  local device="$1"
  curl -i -N -H "Connection: upgrade" -H "Upgrade: websocket" -H 'Host: localhost' "$WS_URI/device/$device"
}

"$@"
