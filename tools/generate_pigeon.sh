#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FLUTTER_DIR="$ROOT_DIR/flutter/aries_ui"

cd "$FLUTTER_DIR"
flutter pub get

dart run pigeon \
  --input "$ROOT_DIR/pigeons/capabilities.dart" \
  --base_path "$ROOT_DIR"

dart format "$ROOT_DIR/flutter/aries_ui/lib/src/generated/capabilities.g.dart"
