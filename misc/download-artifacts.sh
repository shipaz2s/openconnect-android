#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "Downloading prebuilt artifacts is no longer supported." >&2
echo "Build the pinned dependencies from source instead:" >&2
echo "  ${ROOT_DIR}/scripts/build-native-dependencies.sh" >&2
exit 1
