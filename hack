#!/usr/bin/env bash

move() {
  export ID="${ID:-dev-01}"
  export EVENT="${EVENT:-geo}"
  export POS="$1:$2"

  jq -n '{coreid: env.ID, event: env.EVENT, published_at: "now", data: env.POS}' \
  | curl localhost:9000/api/device/dev12345-00001/move -H 'content-type: application/json' -d @-
}

watch() {
  export WS_URI="http://localhost:9000/api/watch"
  "$@"
}

device() {
  local device="$1"
  curl -i -N -H "Connection: upgrade" -H "Upgrade: websocket" -H 'Host: localhost' "$WS_URI/device/$device"
}

"$@"
