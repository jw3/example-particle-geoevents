#!/usr/bin/env bash

readonly pwdir=$(cd $(dirname ${BASH_SOURCE[0]}) && pwd)
readonly dbdir="${DBDIR:-${pwdir}/db.d}"
readonly image="${IMAGE:-crunchydata/crunchy-postgres-gis:centos7-10.3-1.8.2}"
readonly dbname="${DB:-postgres}"
readonly username="${USER:-postgres}"
readonly password="${PASS:-postgres}"
readonly port=5432

start() {
  ${pwdir}/db.d/one-script.sh "$pwdir/target/setup.sql"

  docker run "$@" \
    -p "$port:$port" \
    -e PG_MODE=primary \
    -e PG_DATABASE="$dbname" \
    -e PG_ROOT_PASSWORD=postgres \
    -e PG_PRIMARY_USER="$username" \
    -e PG_PRIMARY_PASSWORD="$password" \
    -e PG_USER=postgres \
    -e PG_PASSWORD=postgres \
    -e PG_PRIMARY_PORT="$port" \
    -v "$pwdir/target/setup.sql":/pgconf/setup.sql \
    ${image}
}

restart() {
  docker start -i "$1"
}

"$@"
