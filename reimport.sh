#!/bin/bash
set -e

psql osm -c "drop table if exists busyness"

psql osm -f createtable.sql

psql osm -f inserts.sql

psql osm -f indexes.sql

