#!/usr/bin/env bash

# Source this file from the repository root:
#   source scripts/dev-env.sh

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    echo "Source this script instead of executing it:" >&2
    echo "  source scripts/dev-env.sh" >&2
    exit 1
fi

DEV_ENV_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEV_TOOLS_DIR="${DEV_ENV_ROOT}/.devtools"

export JAVA_HOME="${DEV_TOOLS_DIR}/jdk-17"
export ANDROID_HOME="${DEV_TOOLS_DIR}/android-sdk"
export ANDROID_USER_HOME="$HOME/.android"
export GRADLE_USER_HOME="${DEV_TOOLS_DIR}/gradle-home"
export ANDROID_NDK_HOME="${ANDROID_HOME}/ndk/27.2.12479018"
export NDK_HOME="${ANDROID_NDK_HOME}"
export PATH="${JAVA_HOME}/bin:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${PATH}"

unset DEV_ENV_ROOT
unset DEV_TOOLS_DIR
