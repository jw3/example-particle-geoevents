#!/usr/bin/env bash

readonly host="${HOST:-${HOSTNAME}}"
readonly port="${PORT:-9000}"

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

  jq -n '{pos: env.POS}' \
  | curl "$host:$port/api/device/$ID/move" -H 'content-type: application/json' -d @-

  echo ""
}

track() {
  curl "$host:$port/api/device/$1/track/$2" -XPOST
  echo ""
}

gettrack() {
  curl "$host:$port/api/track/$1"
  echo ""
}

simulate() {
  local device="$1"
  local number="${2:-2}"

  for p in $(seq 1 ${number}); do
    a=$(( ( RANDOM % 9 ) ))
    b=$(( ( RANDOM % 9 ) ))
    c=$(( ( RANDOM % 9 ) ))
    echo """ move "$device" "39.$a$b" "-79.$c$a" """
    move "$device" "39.$a$b" "-79.$c$a"
  done
}

# usage: ./hack watch device foo1
watch() {
  export WS_URI="http://$host:$port/api/watch"
  "$@"
}

device() {
  local device="$1"
  curl -i -N -H "Connection: upgrade" -H "Upgrade: websocket" -H 'Host: localhost' "$WS_URI/device/$device"
}

"$@"
