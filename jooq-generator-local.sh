#!/usr/bin/env bash
set -euo pipefail

LOCAL_PORT=5432
DB_NAME="nanodb"
DB_USER="nanouser"
DB_PASS="CHANGEME"

DB_URL="jdbc:postgresql://localhost:${LOCAL_PORT}/${DB_NAME}" \
DB_USER="${DB_USER}" \
DB_PASS="${DB_PASS}" \
mvn -Pjooq clean generate-sources verify

echo "[INFO] exit code 0"
