#!/usr/bin/env bash
set -euo pipefail

# === CONFIG ===
VM_USER="deployer"
VM_HOST="34.170.246.31"
SSH_KEY="~/.ssh/nano_ci"
LOCAL_PORT=6543                          # local port for tunnel
DB_NAME="nanodb"
DB_USER="nanouser"
DB_PASS="CHANGEME"

# === OPEN SSH TUNNEL ===
echo "[INFO] Starting SSH tunnel on port ${LOCAL_PORT}..."
ssh -i "${SSH_KEY}" -o StrictHostKeyChecking=no \
    -N -f -M -S /tmp/ssh-sock-jooq \
    -L ${LOCAL_PORT}:localhost:5432 \
    ${VM_USER}@${VM_HOST}

# Verify tunnel is open
sleep 2
if ! nc -z localhost ${LOCAL_PORT}; then
  echo "[ERROR] Tunnel not open on port ${LOCAL_PORT}"
  ssh -S /tmp/ssh-sock-jooq -O exit ${VM_USER}@${VM_HOST} || true
  exit 1
fi

# === RUN JOOQ CODEGEN ===
echo "[INFO] Running jOOQ codegen via tunnel..."
DB_URL="jdbc:postgresql://localhost:${LOCAL_PORT}/${DB_NAME}" \
DB_USER="${DB_USER}" \
DB_PASS="${DB_PASS}" \
mvn -Pjooq clean generate-sources install

# === CLOSE TUNNEL ===
echo "[INFO] Closing SSH tunnel..."
ssh -S /tmp/ssh-sock-jooq -O exit ${VM_USER}@${VM_HOST} || true

echo "[INFO] jOOQ codegen finished successfully."
