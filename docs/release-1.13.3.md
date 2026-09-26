# OpenConnect Split 1.13.3 release notes

Version: `1.13.3` (`versionCode 1133`)

Artifact: `openconnect-split-1.13.3.apk`

SHA-256: `465a44aa4828ffe76da1f22525f2dda85c87f52113f56be3a991b4f8b89fa668`

Signing certificate SHA-256: `bab37b82816ee0ba4f1ea2d0290175af61d8c069da3233654b23b599cabf0169`

## Changes

- The VPN log can be shared from the log screen for easier troubleshooting.
- Stalled TLS handshakes can now be cancelled promptly instead of waiting for
  the GnuTLS handshake timeout.
- Temporary DTLS or ESP loss no longer starts the full-tunnel reconnect timer
  and stops a healthy CSTP connection. OpenConnect can recover the data channel
  without disconnecting the VPN.

## Automated verification

- `git diff --check`: passed.
- Local debug unit tests: passed (`testDebugUnitTest`).
- Debug APK assembly: passed (`assembleDebug`).
- Debug Android lint: passed (`lintDebug`).
- Signed release unit tests, assembly, lint, APK signature verification, and
  SHA-256 generation: passed (`scripts/build-release.sh`).

## Pending device verification

- Confirm log sharing with the installed release build.
- Reproduce intermittent DTLS loss and confirm that the VPN remains connected.
- Confirm cancellation during a stalled TLS handshake.
- Check IPv4, IPv6 when available, DNS, reconnects, per-application routing,
  and subnet split routing on Android 6 and a current Android version.
