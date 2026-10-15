#!/bin/bash
set -e
DEST="/Library/LaunchDaemons/com.appblocker.helper.plist"
sudo launchctl unload "$DEST" 2>/dev/null || true
sudo rm -f "$DEST"
echo "AppBlocker helper daemon removed."
