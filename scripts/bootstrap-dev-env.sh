#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEV_TOOLS_DIR="${ROOT_DIR}/.devtools"
DOWNLOAD_DIR="${DEV_TOOLS_DIR}/downloads"
JDK_DIR="${DEV_TOOLS_DIR}/jdk-17"
ANDROID_HOME="${DEV_TOOLS_DIR}/android-sdk"
GRADLE_HOME="${DEV_TOOLS_DIR}/gradle-home"
ANDROID_USER_HOME="${DEV_TOOLS_DIR}/android-user-home"

ANDROID_COMMAND_LINE_TOOLS_VERSION="11076708"
GRADLE_VERSION="8.10.2"
NDK_VERSION="27.2.12479018"

JDK_ARCHIVE="${DOWNLOAD_DIR}/temurin-jdk-17.tar.gz"
ANDROID_TOOLS_ARCHIVE="${DOWNLOAD_DIR}/commandlinetools-linux-${ANDROID_COMMAND_LINE_TOOLS_VERSION}_latest.zip"
GRADLE_ARCHIVE="${DOWNLOAD_DIR}/gradle-${GRADLE_VERSION}-bin.zip"

JDK_URL="https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse"
ANDROID_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-${ANDROID_COMMAND_LINE_TOOLS_VERSION}_latest.zip"
GRADLE_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"

require_command() {
    if ! command -v "$1" >/dev/null 2>&1; then
        echo "Required host command is missing: $1" >&2
        exit 1
    fi
}

download() {
    local url="$1"
    local destination="$2"

    if [[ -s "${destination}" ]]; then
        echo "Using cached download: ${destination}"
        return
    fi

    echo "Downloading ${url}"
    curl --fail --location --retry 3 --output "${destination}.part" "${url}"
    mv "${destination}.part" "${destination}"
}

require_command curl
require_command tar
require_command unzip

mkdir -p "${DOWNLOAD_DIR}" "${GRADLE_HOME}" "${ANDROID_USER_HOME}"

download "${JDK_URL}" "${JDK_ARCHIVE}"
if [[ ! -x "${JDK_DIR}/bin/java" ]]; then
    mkdir -p "${JDK_DIR}"
    tar -xzf "${JDK_ARCHIVE}" --strip-components=1 -C "${JDK_DIR}"
fi

export JAVA_HOME="${JDK_DIR}"
export ANDROID_HOME
export GRADLE_USER_HOME="${GRADLE_HOME}"
export ANDROID_USER_HOME
export PATH="${JAVA_HOME}/bin:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${PATH}"

download "${ANDROID_TOOLS_URL}" "${ANDROID_TOOLS_ARCHIVE}"
if [[ ! -x "${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager" ]]; then
    ANDROID_EXTRACT_DIR="${DEV_TOOLS_DIR}/android-command-line-tools"
    rm -rf "${ANDROID_EXTRACT_DIR}"
    mkdir -p "${ANDROID_EXTRACT_DIR}" "${ANDROID_HOME}/cmdline-tools"
    unzip -q "${ANDROID_TOOLS_ARCHIVE}" -d "${ANDROID_EXTRACT_DIR}"
    mv "${ANDROID_EXTRACT_DIR}/cmdline-tools" "${ANDROID_HOME}/cmdline-tools/latest"
    rmdir "${ANDROID_EXTRACT_DIR}"
fi

yes | sdkmanager --licenses >/dev/null || true
sdkmanager \
    "platform-tools" \
    "build-tools;34.0.0" \
    "platforms;android-35" \
    "ndk;${NDK_VERSION}"

download "${GRADLE_URL}" "${GRADLE_ARCHIVE}"
if [[ ! -x "${DEV_TOOLS_DIR}/gradle-${GRADLE_VERSION}/bin/gradle" ]]; then
    unzip -q "${GRADLE_ARCHIVE}" -d "${DEV_TOOLS_DIR}"
fi

if [[ ! -x "${ROOT_DIR}/gradlew" || ! -f "${ROOT_DIR}/gradle/wrapper/gradle-wrapper.jar" ]]; then
    "${DEV_TOOLS_DIR}/gradle-${GRADLE_VERSION}/bin/gradle" \
        --project-dir "${ROOT_DIR}" \
        wrapper \
        --gradle-version "${GRADLE_VERSION}" \
        --distribution-type bin
fi

cat >"${ROOT_DIR}/local.properties" <<EOF
sdk.dir=${ANDROID_HOME}
sdk-location=${ANDROID_HOME}
EOF

echo
echo "Local Android development environment is ready."
echo "Activate it with:"
echo "  source scripts/dev-env.sh"
