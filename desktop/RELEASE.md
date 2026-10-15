# Shipping AppBlocker as a downloadable .exe

The installer is **`AppBlocker-Setup-<version>.exe`** — one file. A user downloads
it, double-clicks, and AppBlocker installs like any normal Windows app (Start-menu
entry, desktop shortcut, your icon, everything bundled — no Node, no zip).

The `.exe` must be **compiled on Windows**. You have two ways:

## Option A — Build it yourself (fastest, 1 double-click)
On a Windows PC, in this project folder:
1. Double-click **`build-installer.cmd`** (installs tools + builds).
2. When it finishes, the file is in **`dist\AppBlocker-Setup-<version>.exe`**.
3. That's your release file. Upload it wherever you want people to download it.

## Option B — Let GitHub build it for you (best for "anyone can download")
This repo includes **`.github/workflows/build-windows.yml`**, which builds the
installer on a free Windows cloud machine.
1. Push this project to a GitHub repo.
2. **Build on demand:** GitHub → your repo → **Actions** → *Build Windows
   Installer* → **Run workflow**. When it finishes, download the installer from
   the run's **Artifacts**.
3. **Public download link:** create a version tag and push it:
   ```
   git tag v1.6.0
   git push origin v1.6.0
   ```
   GitHub builds the installer and publishes a **Release** with the `.exe`
   attached. The release page is a public download link you can share anywhere.

## Where to host the download
Any of these work — pick one:
- **GitHub Releases** (free, what Option B produces) — a clean versioned download page.
- **Your site** (`activeviewpoint.com`, Hostinger) — upload the `.exe`, link a
  "Download for Windows" button to it.
- A file host (Dropbox/Drive) — fine for testing, less professional for a product.

## IMPORTANT: the "Unknown publisher" warning (code signing)
An **unsigned** installer makes Windows SmartScreen show
*"Windows protected your PC — Unknown publisher"*. Users must click
**More info → Run anyway**. It still installs, but it looks scary and hurts trust.

To remove it you need a **code-signing certificate**:
- **OV certificate** (~A$150–300/yr from Sectigo, DigiCert, etc.): signs the app;
  the warning clears after your app builds some download "reputation".
- **EV certificate** (dearer, needs a hardware token/HSM): clears SmartScreen
  immediately, no reputation wait.

Once you have a certificate, signing is a config addition in `package.json`:
```json
"win": {
  "certificateFile": "path\\to\\cert.pfx",
  "certificatePassword": "...",     // better: use an env var / CI secret
  "signingHashAlgorithms": ["sha256"]
}
```
Tell me when you have a cert and I'll wire the signing config (and the CI secret
for the GitHub build) so releases come out signed automatically.

## Auto-updates (optional, later)
electron-builder can publish updates so installed apps update themselves. When
you're ready, we add `electron-updater` + a publish target (GitHub Releases works)
so a new tag ships an update to everyone.
