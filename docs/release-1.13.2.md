# OpenConnect Split 1.13.2 release notes

Version: `1.13.2` (`versionCode 1132`)

Artifact: `openconnect-split-1.13.2.apk`

SHA-256: `6dca9a71aeed9e0f0244b8d56d6228ebcecbcb463fc47c88168e06683a64c6fc`

Signing certificate SHA-256: `bab37b82816ee0ba4f1ea2d0290175af61d8c069da3233654b23b599cabf0169`

## Changes

- VPN profiles can be imported from `.ocprof` files containing the server
  settings, CA certificate, client certificate, and private key.
- Imported profiles inherit the per-application routing settings of the last
  used profile.
- Failed imports are rolled back without leaving partial profiles or managed
  certificate files behind.
- Profile imports are limited to 1 MiB and report localized success and error
  messages in English and Russian.
- Debug and release builds use visually distinct launcher icons.
- `scripts/generate-ocprof.sh` generates importable profile files.

## Automated verification

- `git diff --check`: passed.
- Local unit tests: passed (`testDebugUnitTest`).
- Debug APK assembly: passed (`assembleDebug`).
- Debug Android lint: passed (`lintDebug`).
- Signed release unit tests, assembly, lint, APK signature verification, and
  SHA-256 generation: passed (`scripts/build-release.sh`).

## Android 16 device verification

Tested on Xiaomi 24095PCADG, Android 16 (API 36), with `1.13.2-debug`:

- The debug installation was upgraded in place and retained application data.
- An `.ocprof` profile was imported successfully.
- The imported profile was deleted successfully after fixing profile cleanup.
- The signed release APK was installed successfully; Android reported
  `versionCode 1132` and `versionName 1.13.2`.

## Pending verification

- Complete VPN connection checks for IPv4, IPv6 when available, DNS,
  per-application routing, and subnet split routing.
- Import failure and oversized-file rollback on a physical device.
- Android 6 compatibility verification.
