#!/bin/bash
# Downloads the TensorFlowLiteC (C API) xcframework used by the iOS SaliencyEngine
# and vendors just the Core slice into shared/nativeInterop/ (git-ignored).
# Source: the exact tarball the official TensorFlowLiteC CocoaPods podspec points at.
set -euo pipefail

VERSION="2.17.0"
URL="https://dl.google.com/tflite-release/ios/prod/tensorflow/lite/release/ios/release/32/20240729-115310/TensorFlowLiteC/${VERSION}/0c10b3543e01f547/TensorFlowLiteC-${VERSION}.tar.gz"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEST="$ROOT/shared/nativeInterop"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

if [ -d "$DEST/TensorFlowLiteC.xcframework" ]; then
  echo "TensorFlowLiteC.xcframework already present — nothing to do."
  exit 0
fi

echo "Downloading TensorFlowLiteC $VERSION (~76 MB)…"
curl -fSL "$URL" -o "$TMP/tflitec.tar.gz"
echo "Extracting Core xcframework…"
tar xzf "$TMP/tflitec.tar.gz" -C "$TMP" "TensorFlowLiteC-${VERSION}/Frameworks/TensorFlowLiteC.xcframework"
mkdir -p "$DEST"
cp -R "$TMP/TensorFlowLiteC-${VERSION}/Frameworks/TensorFlowLiteC.xcframework" "$DEST/"
echo "Vendored -> $DEST/TensorFlowLiteC.xcframework"
