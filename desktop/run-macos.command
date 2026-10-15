#!/bin/bash
cd "$(dirname "$0")"
echo "AppBlocker - starting up"
if [ ! -d node_modules/electron ]; then
  echo "Installing dependencies (first run only)..."
  npm install
fi
if [ ! -e node_modules/electron/dist/Electron.app ] && [ ! -e node_modules/electron/dist/electron ]; then
  echo "Downloading Electron (first run only)..."
  node node_modules/electron/install.js
fi
echo "Starting AppBlocker (sudo needed for website blocking)..."
sudo npm start
