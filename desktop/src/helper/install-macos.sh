#!/bin/bash
# Run with sudo AFTER moving AppBlocker.app to /Applications.
# Installs a launchd daemon (runs as root) that keeps website blocking applied.
set -e
DIR="$(cd "$(dirname "$0")" && pwd)"
DEST="/Library/LaunchDaemons/com.appblocker.helper.plist"
sudo cp "$DIR/com.appblocker.helper.plist" "$DEST"
sudo chown root:wheel "$DEST"
sudo chmod 644 "$DEST"
sudo launchctl unload "$DEST" 2>/dev/null || true
sudo launchctl load "$DEST"
echo "AppBlocker helper daemon installed."
