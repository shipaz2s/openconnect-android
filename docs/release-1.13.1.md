# OpenConnect Split 1.13.1 release notes

Version: `1.13.1` (`versionCode 1131`)

Planned artifact: `openconnect-split-1.13.1.apk`

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

## Required device verification

- Android 6 and a current targetSdk-level Android version.
- Successful connect/disconnect and cancellation during initial connection and authentication prompts.
- Failed reconnect stops after the configured timeout and clears the Android VPN indicator.
- Quick Settings connects the last profile, cancels an attempt, and disconnects an active tunnel.
- IPv4, IPv6 when available, DNS, per-app allowlist, and subnet split routing.
- Removal of the last selected app while disconnected and while connected (effective after reconnect).
