App icons (already generated):
  icon.ico   - Windows app + installer icon (multi-size 16-256)
  icon.icns  - macOS app icon
  icon.png   - 256px master (used for the window icon)
  icon-512.png - 512px master
  tray.png / trayTemplate.png - system tray icon

To regenerate, re-run the icon script. To change the art, replace these files
(keep the same names and sizes) and rebuild with: npm run dist:win
