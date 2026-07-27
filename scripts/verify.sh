#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# shellcheck source=dev-env.sh
source "${ROOT_DIR}/scripts/dev-env.sh"

if [[ ! -x "${ROOT_DIR}/gradlew" ]]; then
    echo "Gradle wrapper is missing. Run scripts/bootstrap-dev-env.sh first." >&2
    exit 1
fi

if [[ ! -f "${ROOT_DIR}/app/libs/openconnect-wrapper.jar" ||
      ! -f "${ROOT_DIR}/app/libs/stoken-wrapper.jar" ||
      ! -f "${ROOT_DIR}/app/src/main/jniLibs/arm64-v8a/libopenconnect.so" ]]; then
    echo "Application dependencies are missing." >&2
    echo "Run scripts/build-native-dependencies.sh first." >&2
    exit 1
fi

cd "${ROOT_DIR}"

./gradlew testDebugUnitTest --no-daemon
./gradlew lint --no-daemon
./gradlew assembleDebug --no-daemon
