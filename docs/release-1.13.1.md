# OpenConnect Split 1.13.1 release notes

Version: `1.13.1` (`versionCode 1131`)

Artifact: `openconnect-split-1.13.1.apk`

SHA-256: `d64a593e156122987d5da20ba76085ec1ce090ce77c9a2c971fe7b6cf7da5990`

Signing certificate SHA-256: `bab37b82816ee0ba4f1ea2d0290175af61d8c069da3233654b23b599cabf0169`

## Changes

- Automatic recovery after a dropped VPN connection is bounded instead of retrying indefinitely.
- The reconnect timeout defaults to 30 seconds and can be changed under Settings to 0, 10, 30, 60, 120, or 300 seconds.
- An in-progress connection can be cancelled from the status screen, log screen, or Quick Settings tile, including while an authentication or certificate dialog is pending.
- The Quick Settings tile is active only in the connected state; tapping it connects the last used profile or disconnects/cancels the current attempt.
- The last used profile is persisted independently from the currently connected profile and migrated from the previous service preference.
- The About screen shows the installed version, project repository, and support email.

## Automated verification

- `git diff --check`: passed.
- Local unit tests: passed (`testDebugUnitTest`).
- Debug APK assembly: passed (`assembleDebug`).
- Android lint: passed (`lint`).
- Signed release unit tests, assembly, lint, and APK signature verification: passed (`scripts/build-release.sh`).

## Android 16 device verification

Tested on Xiaomi 24095PCADG, Android 16 (API 36), with `1.13.1-debug`:

- The existing debug installation was upgraded in place and retained its profiles.
- Repeated cancellation during `CONNECTING` and immediately after `CONNECTED` returned to `DISCONNECTED`; the VPN thread terminated and Android reported no active VPN network for the debug package.
- The Quick Settings tile connected the last used profile, became active only after connection, and disconnected the tunnel on the next tap.

The bounded reconnect timeout still requires a controlled test with an available underlying network and an unreachable VPN gateway. Android 6 verification remains pending.

## Required device verification

- Android 6 and a current targetSdk-level Android version.
- Successful connect/disconnect and cancellation during initial connection and authentication prompts.
- Failed reconnect stops after the configured timeout and clears the Android VPN indicator.
- Quick Settings connects the last profile, cancels an attempt, and disconnects an active tunnel.
- IPv4, IPv6 when available, DNS, per-app allowlist, and subnet split routing.
- Removal of the last selected app while disconnected and while connected (effective after reconnect).
