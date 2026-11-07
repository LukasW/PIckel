#!/usr/bin/env bash
# Richtet den Devcontainer nach dem Erstellen ein.
# node_modules liegt auf einem Container-Volume, damit keine macOS-Binaries in Linux landen.
set -euo pipefail

# Unter Podman hinterlässt das Java-Feature /tmp mit 0755 statt 1777; ohne Sticky-Bit und
# Schreibrecht scheitern apt (Benutzer _apt), Maven, npm und Claude Code. Hier für den ersten
# Durchlauf, postStartCommand in devcontainer.json wiederholt es bei jedem Start.
sudo chmod 1777 /tmp

# jq: Ohne jq halten die Hooks unter .claude/flow/scripts/ an (fail-closed)
command -v jq >/dev/null 2>&1 || { sudo apt-get update && sudo apt-get install -y jq; }

# pre-commit-Hook (Gitleaks, Prettier, Spotless) aktivieren
git -C "${WORKSPACE}" config core.hooksPath .githooks

sudo chown "$(id -u):$(id -g)" "${WORKSPACE}/src/main/webapp/node_modules"
sudo npm install -g npm@11.17.0
npm --prefix "${WORKSPACE}/src/main/webapp" ci

# Claude Code auf den nativen Installer umstellen, damit Updates ohne sudo gehen.
if command -v claude >/dev/null 2>&1; then
  claude install || true
fi
