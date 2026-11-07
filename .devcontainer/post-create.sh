#!/usr/bin/env bash
# Richtet den Devcontainer nach dem Erstellen ein.
# node_modules liegt auf einem Container-Volume, damit keine macOS-Binaries in Linux landen.
set -euo pipefail

# jq: Ohne jq halten die Hooks unter .claude/flow/scripts/ an (fail-closed)
command -v jq >/dev/null 2>&1 || { sudo apt-get update && sudo apt-get install -y jq; }

# pre-commit-Hook (Gitleaks, Prettier, Spotless) aktivieren
git -C "${WORKSPACE}" config core.hooksPath .githooks

sudo chown "$(id -u):$(id -g)" "${WORKSPACE}/src/main/webapp/node_modules" "${WORKSPACE}/.quinoa"

# Volumes und die von Docker angelegten Elternverzeichnisse gehören zuerst root
sudo chown "$(id -u):$(id -g)" "${HOME}/.config" "${HOME}/.config/gh" "${HOME}/.claude"
# Ohne sudo: sudo setzt PATH auf secure_path zurück und findet das npm aus nvm nicht. Das Node-Feature
# nimmt den Benutzer in die Gruppe nvm auf, globale Installationen gehen deshalb ohne root.
npm install -g npm@11.17.0
npm --prefix "${WORKSPACE}/src/main/webapp" ci

# Claude Code auf den nativen Installer umstellen, damit Updates ohne sudo gehen.
if command -v claude >/dev/null 2>&1; then
  claude install || true
fi
