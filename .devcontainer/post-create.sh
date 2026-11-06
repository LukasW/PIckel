#!/usr/bin/env bash
# Richtet den Devcontainer nach dem Erstellen ein.
# node_modules liegt auf einem Container-Volume, damit keine macOS-Binaries in Linux landen.
set -euo pipefail

sudo chown "$(id -u):$(id -g)" "${WORKSPACE}/src/main/webapp/node_modules"
sudo npm install -g npm@11.17.0
npm --prefix "${WORKSPACE}/src/main/webapp" ci

# Claude Code auf den nativen Installer umstellen, damit Updates ohne sudo gehen.
if command -v claude >/dev/null 2>&1; then
  claude install || true
fi
