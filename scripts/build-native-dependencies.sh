#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_DIR="${ROOT_DIR}/.devtools/wrapper-classes"

# shellcheck source=dev-env.sh
source "${ROOT_DIR}/scripts/dev-env.sh"

if [[ ! -x "${ANDROID_NDK_HOME}/toolchains/llvm/prebuilt/linux-x86_64/bin/clang" ]]; then
    echo "Android NDK r27c is missing from ${ANDROID_NDK_HOME}." >&2
    echo "Run scripts/bootstrap-dev-env.sh first." >&2
    exit 1
fi

for command in autoconf automake autopoint bison flex javac jar libtoolize \
    make msgfmt pkg-config xgettext; do
    if ! command -v "${command}" >/dev/null 2>&1; then
        echo "Required host command is missing: ${command}" >&2
        exit 1
    fi
done

if (($# == 0)); then
    architectures=(arm arm64)
else
    architectures=("$@")
fi

for architecture in "${architectures[@]}"; do
    case "${architecture}" in
        arm|arm64|x86|x86_64) ;;
        *)
            echo "Unsupported architecture: ${architecture}" >&2
            echo "Expected one of: arm arm64 x86 x86_64" >&2
            exit 1
            ;;
    esac
done

make_args=()
for architecture in "${architectures[@]}"; do
    make_args+=("install-arch-${architecture}")
done

make \
    -C "${ROOT_DIR}/external" \
    NDK="${ANDROID_NDK_HOME}" \
    "${make_args[@]}"

rm -rf "${BUILD_DIR}/openconnect" "${BUILD_DIR}/stoken"
mkdir -p \
    "${BUILD_DIR}/openconnect" \
    "${BUILD_DIR}/stoken" \
    "${ROOT_DIR}/app/libs"

javac \
    -source 8 \
    -target 8 \
    -d "${BUILD_DIR}/openconnect" \
    "${ROOT_DIR}/external/openconnect/java/src/org/infradead/libopenconnect/LibOpenConnect.java"

jar --create \
    --file "${ROOT_DIR}/app/libs/openconnect-wrapper.jar" \
    -C "${BUILD_DIR}/openconnect" org/infradead/libopenconnect

javac \
    -source 8 \
    -target 8 \
    -d "${BUILD_DIR}/stoken" \
    "${ROOT_DIR}/external/stoken/java/src/org/stoken/LibStoken.java"

jar --create \
    --file "${ROOT_DIR}/app/libs/stoken-wrapper.jar" \
    -C "${BUILD_DIR}/stoken" org/stoken

echo "Native dependencies and Java wrappers were built from source."
