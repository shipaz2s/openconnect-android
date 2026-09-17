# Connection resilience release verification

Release: `v1.13-split`

Artifact: `openconnect-split-1.13.apk`

SHA-256:
`001c70a1b6ac71f3f22ae84e0afddc7d35fe4ea6e8a6997b86952fe69751b071`

Signing certificate SHA-256:
`bab37b82816ee0ba4f1ea2d0290175af61d8c069da3233654b23b599cabf0169`

## Automated verification

- `testDebugUnitTest` passed.
- Android lint passed.
- Debug APK assembly passed.
- Underlying-network state transitions and dead-peer message recognition are
  covered by local unit tests.

## Physical-device verification

Tested on Android API 36 with the parallel-installable `.split` debug package:

- Wi-Fi to cellular paused and resumed the existing tunnel.
- Cellular to Wi-Fi produced one debounced reconnect.
- Complete network loss displayed `Waiting for network` without a reconnect
  storm.
- Restoring connectivity resumed the tunnel and returned to `Connected`.
- IPv4, IPv6, DNS, routes, and per-app UID ranges remained installed.
- A server-requested disconnect terminated normally and was not retried.

Android 6 physical-device verification was intentionally deferred and is not a
