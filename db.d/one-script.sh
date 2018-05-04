#!/usr/bin/env bash

readonly pwdir=$(cd $(dirname ${BASH_SOURCE[0]}) && pwd)
readonly target="$1"

echo "-- combined sql scripts" > "$target"
echo "" >> "$target"
for f in $(ls ${pwdir}/*.sql); do
  echo "-- ############## ${f} ##############" >> "$target"
  cat "$f" >> "$target"
  echo "" >> "$target"
done
chmod 777 "$target"
