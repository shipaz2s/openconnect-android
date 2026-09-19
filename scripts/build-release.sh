#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROPERTIES_FILE="${ROOT_DIR}/release.properties"

# shellcheck source=dev-env.sh
source "${ROOT_DIR}/scripts/dev-env.sh"

if [[ ! -f "${PROPERTIES_FILE}" ]]; then
    echo "Missing ${PROPERTIES_FILE}." >&2
    echo "Copy release.properties.example and fill in the signing values." >&2
    exit 1
fi

for key in storeFile storePassword keyAlias keyPassword; do
    if ! grep -q "^${key}=..*" "${PROPERTIES_FILE}"; then
        echo "Missing release signing property: ${key}" >&2
        exit 1
    fi
done

if grep -q "CHANGE_ME" "${PROPERTIES_FILE}"; then
    echo "Replace CHANGE_ME values in release.properties." >&2
    exit 1
fi

cd "${ROOT_DIR}"
./gradlew clean testReleaseUnitTest lintRelease assembleRelease --no-daemon

APK="${ROOT_DIR}/app/build/outputs/apk/release/openconnect-split-1.13.1.apk"
if [[ ! -f "${APK}" ]]; then
    echo "Expected release APK was not produced: ${APK}" >&2
    exit 1
fi

"${ANDROID_HOME}/build-tools/34.0.0/apksigner" verify \
    --verbose \
    --print-certs \
    "${APK}"
sha256sum "${APK}"
