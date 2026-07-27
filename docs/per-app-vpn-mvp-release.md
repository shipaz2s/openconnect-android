# Per-application VPN MVP release

Status: ready for integration into `main`.

## Included functionality

- Per-profile `Only selected applications` mode.
- Searchable selector for launcher applications.
- Per-profile persistence of selected Android package names.
- Missing saved packages remain visible in the selector and can be removed.
- Application allowlist is applied with
  `VpnService.Builder.addAllowedApplication()` before `establish()`.
- Fail-closed behavior when the mode is enabled with an empty allowlist or when
  none of the selected packages are installed.
- Existing profiles retain the original all-applications behavior by default.
- Existing route-based split tunneling remains independent and can be combined
  with application filtering.
- Android 6 remains the minimum supported version (`minSdk 23`).
- Android 11+ package visibility is scoped to launcher applications; the MVP
  does not request `QUERY_ALL_PACKAGES`.
- The debug application uses the `.split` application ID suffix and can be
  installed alongside the original client.
- Native OpenConnect, stoken, curl, and `run_pie` artifacts are built from
  source for ARMv7 and ARM64.

## Verification completed

- `testDebugUnitTest` passes, including allowlist validation tests.
- Android Lint passes.
- Clean debug APK assembly passes.
- The APK contains only source-built ARMv7 and ARM64 native artifacts.
- Installation and launch on a physical ARM64 Android device succeeds.
- Manual device test confirmed:
  - a selected application uses the VPN;
  - a non-selected application uses the regular uplink;
  - the VPN connection remains operational with application filtering enabled.

## Deferred validation

These scenarios are not release blockers for the first MVP, but should be
covered before wider distribution:

- physical Android 6 device;
- removal of a selected application while configured;
- IPv6 routing;
- always-on VPN and lockdown mode;
- Android TV application selector.

## MVP limitations

- Only allowlist mode is implemented; there is no “all except selected” mode.
- Only launcher applications are shown on Android 11 and newer.
- Changes made while connected require a manual reconnect.
- Android TV-specific selector work is deferred.
