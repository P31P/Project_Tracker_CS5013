#!/bin/sh
# Starts the image's own PostgreSQL, creates the demo database on the first run, then starts the tracker.
set -e

PG_BIN=$(ls -d /usr/lib/postgresql/*/bin)
DATA=/var/lib/postgresql/demo

if [ ! -s "$DATA/PG_VERSION" ]; then
    mkdir -p "$DATA"
    chown postgres "$DATA"
    # only reachable from inside the container, so local connections need no password
    su postgres -c "$PG_BIN/initdb -D $DATA -A trust" > /dev/null
    FIRST_RUN=yes
fi

su postgres -c "$PG_BIN/pg_ctl -D $DATA -l /tmp/postgres.log -o '-c listen_addresses=localhost -c unix_socket_directories=/tmp' -w start"

if [ "$FIRST_RUN" = yes ]; then
    # demo.marker tells demo mode it is on this database; the app loads the sample data once Flyway has run
    su postgres -c "psql -q -h localhost -v ON_ERROR_STOP=1" <<'SQL'
CREATE ROLE tracker LOGIN PASSWORD 'tracker';
CREATE DATABASE tracker OWNER tracker;
\c tracker
CREATE SCHEMA demo AUTHORIZATION tracker;
CREATE TABLE demo.marker (seeded BOOLEAN NOT NULL);
INSERT INTO demo.marker VALUES (false);
ALTER TABLE demo.marker OWNER TO tracker;
SQL
fi

exec java -jar /app/tracker.jar
