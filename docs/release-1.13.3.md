# OpenConnect Split 1.13.3 release notes

Version: `1.13.3` (`versionCode 1133`)

Artifact: `openconnect-split-1.13.3.apk`

SHA-256: `bd2497ba297246bebf823953d3fc1dfdb132fd73bf4a1e346370f8ababb6a5f6`

Signing certificate SHA-256: `bab37b82816ee0ba4f1ea2d0290175af61d8c069da3233654b23b599cabf0169`

## Changes

- The VPN log can be shared from the log screen for easier troubleshooting.
- Stalled TLS handshakes can now be cancelled promptly instead of waiting for
  the GnuTLS handshake timeout.
- TLS handshakes retain GnuTLS' 40-second default timeout instead of failing
  immediately when no handshake data is ready yet.
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

## Manual verification

- The signed release APK was installed as an update on Android 11 (API 30),
  preserving the existing profile, and successfully connected to the VPN.
