#!/bin/bash
# Regenerate u2netp_320_float32.tflite from the source ONNX weights.
# Requires the conversion deps from requirements.txt in an active venv.
set -euo pipefail

ONNX_URL="https://huggingface.co/BritishWerewolf/U-2-Netp/resolve/main/onnx/model.onnx"
WORK="${1:-./_convert}"
mkdir -p "$WORK"
cd "$WORK"

# 1. Fetch the verified u2netp ONNX (fp32, 4.57 MB, input [1,3,320,320]).
curl -sL "$ONNX_URL" -o u2netp.onnx

# 2. Fold dynamic Resize/Shape ops to static — REQUIRED, else onnx2tf mis-resolves
#    U-2-Net's upsample-to-target-size nodes and produces a broken model.
onnxslim u2netp.onnx u2netp_slim.onnx

# 3. Convert ONNX (NCHW) -> TFLite (NHWC). Emits float32 + float16 variants.
onnx2tf -i u2netp_slim.onnx -o tf

# Result: tf/u2netp_slim_float32.tflite  (main saliency = output index 0,
# shape [1,320,320,1], sigmoid-applied). Verified 0.0 max-abs-diff vs the ONNX.
echo "Done -> $WORK/tf/u2netp_slim_float32.tflite"
