#!/usr/bin/env bash
set -euo pipefail

# === CONFIG ===
VM_HOST="127.0.0.1"
LOCAL_PORT=5432                          # local port for tunnel
DB_NAME="nanodb"
DB_USER="nanouser"
DB_PASS="CHANGEME"

# === RUN JOOQ CODEGEN ===
echo "[INFO] Running jOOQ codegen locally..."
DB_URL="jdbc:postgresql://localhost:${LOCAL_PORT}/${DB_NAME}" \
DB_USER="${DB_USER}" \
DB_PASS="${DB_PASS}" \
mvn -Pjooq clean generate-sources install

echo "[INFO] jOOQ codegen finished successfully."
