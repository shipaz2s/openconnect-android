#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_DIR="${ROOT_DIR}/.devtools/wrapper-classes"

# shellcheck source=dev-env.sh
source "${ROOT_DIR}/scripts/dev-env.sh"

cd "${ROOT_DIR}"

./misc/download-artifacts.sh

mkdir -p \
    "${BUILD_DIR}/openconnect" \
    "${BUILD_DIR}/stoken" \
    "${ROOT_DIR}/app/libs"

javac \
    -source 8 \
    -target 8 \
    -d "${BUILD_DIR}/openconnect" \
    external/openconnect/java/src/org/infradead/libopenconnect/LibOpenConnect.java

jar --create \
    --file "${ROOT_DIR}/app/libs/openconnect-wrapper.jar" \
    -C "${BUILD_DIR}/openconnect" org/infradead/libopenconnect

javac \
    -source 8 \
    -target 8 \
    -d "${BUILD_DIR}/stoken" \
    external/stoken/java/src/org/stoken/LibStoken.java

jar --create \
    --file "${ROOT_DIR}/app/libs/stoken-wrapper.jar" \
    -C "${BUILD_DIR}/stoken" org/stoken

echo "Prebuilt native dependencies and source-matched Java wrappers are ready."
