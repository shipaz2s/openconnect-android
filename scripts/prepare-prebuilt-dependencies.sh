#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "prepare-prebuilt-dependencies.sh now builds dependencies from source."
exec "${ROOT_DIR}/scripts/build-native-dependencies.sh" "$@"
