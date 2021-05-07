#!/usr/bin/env bash

# https://stackoverflow.com/a/246128
readonly script_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

readonly host="${HOST:-${HOSTNAME}}"
readonly port="${PORT:-9000}"

add() {
  curl "$host:$port/api/device/$1" -XPOST
  echo ""
}

fence() {
  curl "$host:$port/api/fence" -d @"$script_dir/data/fences.geojson" -H "Content-Type: application/json"
  echo ""
}

health() {
  curl "$host:$port/api/health"
  echo ""
}

# take coords in lat lon format
move() {
  export ID="$1"
  export X="$2"
  export Y="$3"

  jq -n '{id: env.ID, x: env.X, y: env.Y}' \
  | curl "$host:$port/api/device/move" -H 'content-type: application/json' -d @-

  echo ""
}

hook() {
  local device=${1:-foo}
  local a=$(( ( RANDOM % 9 ) ))
  local b=$(( ( RANDOM % 9 ) ))
  local c=$(( ( RANDOM % 9 ) ))
  echo """ hook moves $device 39.$a$b -79.$c$a """

  local foo=$(cat  <<HOOK
{
  "id": "$device",
  "event": "M",
  "data": "39.$a$b:-79.$c$a",
  "when": "99999"
}
HOOK
)
  curl "$host:$port/api/device/move" -H 'content-type: application/json' -d "$foo"

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
    echo """ move $device 39.$a$b -79.$c$a """
    move "$device" "39.$a$b" "-79.$c$a" &> /dev/null
    sleep 1
  done
}

watch() {
  websocat "ws://$host:$port/api/watch/device"
}


"$@"
