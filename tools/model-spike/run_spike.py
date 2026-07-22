#!/usr/bin/env python3
"""
Milestone 3 — Model spike for u2netp saliency-based smart cropping.

Runs the *same* pipeline the KMP app will run on-device:
  RGB image -> resize 320x320 -> /255 -> ImageNet normalize -> u2netp.tflite
  -> saliency mask -> CropCalculator (ported 1:1 from the Kotlin shared module)
  -> CropRegion.

For each input image it writes a 3-panel montage to --out:
  [ original | saliency heatmap | original + computed crop box ]

The model is a float32 TFLite converted from the verified HuggingFace u2netp
ONNX (BritishWerewolf/U-2-Netp); see README.md for provenance/conversion.

Usage:
  run_spike.py --model u2netp_float32.tflite --avatars ./avatars --out ./out \
               --aspect 1.0 --threshold 0.5
"""
import argparse
import glob
import os

import numpy as np
from PIL import Image, ImageDraw
from ai_edge_litert.interpreter import Interpreter

# ImageNet normalization from the model's preprocessor_config.json.
MEAN = np.array([0.485, 0.456, 0.406], dtype=np.float32)
STD = np.array([0.229, 0.224, 0.225], dtype=np.float32)
INPUT_SIZE = 320


# ---------------------------------------------------------------------------
# CropCalculator — a faithful Python port of
# shared/src/commonMain/kotlin/com/smartcrop/shared/ml/CropCalculator.kt
# Keep this in sync with the Kotlin source; the spike is only meaningful if the
# crop math matches the app exactly.
# ---------------------------------------------------------------------------
CENTER = dict(x=0.0, y=0.0, width=1.0, height=1.0, confidence=0.0)


def compute_crop_region(mask, mask_w, mask_h, target_aspect, threshold=0.5):
    if mask_w <= 0 or mask_h <= 0:
        return CENTER
    if mask.size != mask_w * mask_h:
        return CENTER
    if not (target_aspect > 0.0) or not np.isfinite(target_aspect):
        return CENTER

    m = mask.reshape(mask_h, mask_w)
    salient = m >= threshold
    if not salient.any():
        return CENTER
    rows = np.where(salient.any(axis=1))[0]
    cols = np.where(salient.any(axis=0))[0]
    min_row, max_row = int(rows[0]), int(rows[-1])
    min_col, max_col = int(cols[0]), int(cols[-1])

    w_f, h_f = float(mask_w), float(mask_h)
    box_x = min_col / w_f
    box_y = min_row / h_f
    box_w = (max_col - min_col + 1) / w_f
    box_h = (max_row - min_row + 1) / h_f

    center_x = box_x + box_w / 2.0
    center_y = box_y + box_h / 2.0

    normalized_ratio = target_aspect * (h_f / w_f)
    current_ratio = box_w / box_h
    if current_ratio < normalized_ratio:
        box_w = box_h * normalized_ratio
    elif current_ratio > normalized_ratio:
        box_h = box_w / normalized_ratio

    box_w = min(box_w, 1.0)
    box_h = min(box_h, 1.0)

    box_x = center_x - box_w / 2.0
    box_y = center_y - box_h / 2.0
    if box_x < 0.0:
        box_x = 0.0
    if box_x + box_w > 1.0:
        box_x = 1.0 - box_w
    if box_y < 0.0:
        box_y = 0.0
    if box_y + box_h > 1.0:
        box_y = 1.0 - box_h

    start_col = min(max(int(box_x * w_f), 0), mask_w - 1)
    end_col = min(max(int((box_x + box_w) * w_f), 0), mask_w)
    start_row = min(max(int(box_y * h_f), 0), mask_h - 1)
    end_row = min(max(int((box_y + box_h) * h_f), 0), mask_h)
    sub = m[start_row:end_row, start_col:end_col]
    confidence = float(np.clip(sub.mean(), 0.0, 1.0)) if sub.size else 0.0

    return dict(x=box_x, y=box_y, width=box_w, height=box_h, confidence=confidence)


# ---------------------------------------------------------------------------
# Inference + visualization
# ---------------------------------------------------------------------------
def preprocess(img):
    """RGB PIL image -> NHWC float32 tensor, ImageNet-normalized."""
    im = img.convert("RGB").resize((INPUT_SIZE, INPUT_SIZE), Image.BILINEAR)
    arr = np.asarray(im, dtype=np.float32) / 255.0
    arr = (arr - MEAN) / STD
    return arr[np.newaxis, ...]  # [1,320,320,3]


def saliency(interp, tensor):
    """Run the model, return the main saliency map as [H,W] float in [0,1]."""
    inp = interp.get_input_details()[0]
    out = interp.get_output_details()
    interp.set_tensor(inp["index"], tensor)
    interp.invoke()
    # Output index 0 is the composite/main map (1959 in the ONNX); verified to
    # match ONNX to 0.0 abs diff. Others are the 6 side outputs.
    m = np.squeeze(interp.get_tensor(out[0]["index"])).astype(np.float32)
    return m  # already sigmoid-activated, in [0,1]


def normalize_minmax(m):
    lo, hi = float(m.min()), float(m.max())
    return (m - lo) / (hi - lo) if hi > lo else np.zeros_like(m)


def heatmap(mask01, size):
    """Grayscale->simple blue/red heatmap PIL image at the given size."""
    m = (mask01 * 255).astype(np.uint8)
    im = Image.fromarray(m, mode="L").resize(size, Image.BILINEAR)
    g = np.asarray(im, dtype=np.float32) / 255.0
    rgb = np.stack([g, np.zeros_like(g), 1.0 - g], axis=-1)  # salient=red, bg=blue
    return Image.fromarray((rgb * 255).astype(np.uint8), mode="RGB")


def montage(orig, mask01, crop, aspect):
    w, h = orig.size
    panel_mask = heatmap(mask01, (w, h))
    panel_box = orig.copy()
    d = ImageDraw.Draw(panel_box)
    bx, by = crop["x"] * w, crop["y"] * h
    bw, bh = crop["width"] * w, crop["height"] * h
    d.rectangle([bx, by, bx + bw, by + bh], outline=(0, 255, 0), width=4)
    out = Image.new("RGB", (w * 3 + 16, h), (20, 20, 20))
    out.paste(orig, (0, 0))
    out.paste(panel_mask, (w + 8, 0))
    out.paste(panel_box, (w * 2 + 16, 0))
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--model", required=True)
    ap.add_argument("--avatars", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--aspect", type=float, default=1.0, help="target width/height")
    ap.add_argument("--threshold", type=float, default=0.5)
    ap.add_argument("--normalize", choices=["minmax", "raw"], default="minmax")
    args = ap.parse_args()

    os.makedirs(args.out, exist_ok=True)
    interp = Interpreter(model_path=args.model)
    interp.allocate_tensors()

    paths = sorted(glob.glob(os.path.join(args.avatars, "*.jpeg")) +
                   glob.glob(os.path.join(args.avatars, "*.jpg")) +
                   glob.glob(os.path.join(args.avatars, "*.png")))
    print(f"{len(paths)} images | aspect={args.aspect} threshold={args.threshold} "
          f"norm={args.normalize}\n")
    print(f"{'image':<18}{'salient%':>9}{'conf':>7}   crop(x,y,w,h)")
    for p in paths:
        orig = Image.open(p).convert("RGB")
        raw = saliency(interp, preprocess(orig))
        mask = normalize_minmax(raw) if args.normalize == "minmax" else raw
        crop = compute_crop_region(mask.flatten(), mask.shape[1], mask.shape[0],
                                   args.aspect, args.threshold)
        salient_pct = 100.0 * float((mask >= args.threshold).mean())
        name = os.path.basename(p)
        print(f"{name:<18}{salient_pct:>8.1f}%{crop['confidence']:>7.2f}   "
              f"({crop['x']:.2f},{crop['y']:.2f},{crop['width']:.2f},{crop['height']:.2f})")
        montage(orig, mask, crop, args.aspect).save(
            os.path.join(args.out, f"montage_{os.path.splitext(name)[0]}.png"))
    print(f"\nMontages written to {args.out}")


if __name__ == "__main__":
    main()
