package com.smartcrop.shared.ml

import com.smartcrop.shared.domain.model.CropRegion

/**
 * Pure, deterministic crop-math used to turn a saliency mask into a [CropRegion].
 *
 * Pipeline: saliency mask -> thresholded salient bounding box -> expand/pad to the
 * target aspect ratio -> clamp to image bounds -> [CropRegion] with a confidence score.
 *
 * Coordinates in the returned [CropRegion] are normalized to the range `[0, 1]`
 * (`x, y` = top-left corner; `width, height` = fractions of the image).
 *
 * Aspect ratio note: `targetAspectRatio` is expressed in *pixel* space (image
 * `width / height`). Because the mask can be non-square, the equivalent ratio in
 * normalized space is `targetAspectRatio * maskHeight / maskWidth`.
 */
object CropCalculator {

    /**
     * Compute a crop region from a saliency mask.
     *
     * @param mask row-major saliency values, length must equal `maskWidth * maskHeight`,
     *   each value expected to be in `[0, 1]`.
     * @param maskWidth width of the mask in pixels (must be > 0).
     * @param maskHeight height of the mask in pixels (must be > 0).
     * @param targetAspectRatio desired crop `width / height` in pixel space, e.g. `16f / 9f`.
     * @param threshold saliency value at or above which a pixel counts as salient.
     * @param minSize minimum crop size as a fraction of each image dimension `[0, 1]`.
     *   Guards against over-zooming on tiny/distant salient objects (a finding from
     *   the real-photo model spike); the crop is scaled up equally on both axes so
     *   neither side falls below this. `0` disables the guard.
     * @return the computed [CropRegion], or [CropRegion.CENTER] when the input is invalid
     *   or no pixel passes the threshold.
     */
    fun computeCropRegion(
        mask: FloatArray,
        maskWidth: Int,
        maskHeight: Int,
        targetAspectRatio: Float,
        threshold: Float = 0.5f,
        minSize: Float = 0f,
    ): CropRegion {
        // Guard against invalid inputs.
        if (maskWidth <= 0 || maskHeight <= 0) return CropRegion.CENTER
        if (mask.size != maskWidth * maskHeight) return CropRegion.CENTER
        if (targetAspectRatio <= 0f || !targetAspectRatio.isFinite()) return CropRegion.CENTER

        // 1. Scan the mask for the tight bounding box of salient pixels.
        var minCol = maskWidth
        var minRow = maskHeight
        var maxCol = -1
        var maxRow = -1
        var salientCount = 0
        for (row in 0 until maskHeight) {
            val rowOffset = row * maskWidth
            for (col in 0 until maskWidth) {
                if (mask[rowOffset + col] >= threshold) {
                    if (col < minCol) minCol = col
                    if (col > maxCol) maxCol = col
                    if (row < minRow) minRow = row
                    if (row > maxRow) maxRow = row
                    salientCount++
                }
            }
        }

        // 2. Nothing salient -> fall back to a full-image center crop.
        if (maxCol < 0 || maxRow < 0 || salientCount == 0) return CropRegion.CENTER

        // 3. Convert the pixel bbox to normalized coords. A pixel at column `c`
        //    spans the normalized range [c / w, (c + 1) / w], so the inclusive
        //    bbox [minCol, maxCol] spans [minCol / w, (maxCol + 1) / w].
        val wF = maskWidth.toFloat()
        val hF = maskHeight.toFloat()
        var boxX = minCol / wF
        var boxY = minRow / hF
        var boxW = (maxCol - minCol + 1) / wF
        var boxH = (maxRow - minRow + 1) / hF

        val centerX = boxX + boxW / 2f
        val centerY = boxY + boxH / 2f

        // Target ratio expressed in normalized space (boxW / boxH == normalizedRatio).
        val normalizedRatio = targetAspectRatio * (hF / wF)

        // Expand the smaller dimension so the box matches the target ratio,
        // keeping it centered on the salient bbox center.
        val currentRatio = boxW / boxH
        if (currentRatio < normalizedRatio) {
            // Too tall for the target -> widen.
            boxW = boxH * normalizedRatio
        } else if (currentRatio > normalizedRatio) {
            // Too wide for the target -> heighten.
            boxH = boxW / normalizedRatio
        }

        // Enforce a minimum crop size so tiny/distant salient objects don't
        // over-zoom. Scale both axes by the same factor to preserve the ratio.
        if (minSize > 0f) {
            val scale = maxOf(1f, minSize / boxW, minSize / boxH)
            boxW *= scale
            boxH *= scale
        }

        // 4. Clamp to [0, 1]. Prefer shifting the box to stay in bounds; only
        //    shrink a side (accepting ratio distortion) when it cannot fit.
        if (boxW > 1f) boxW = 1f
        if (boxH > 1f) boxH = 1f

        boxX = centerX - boxW / 2f
        boxY = centerY - boxH / 2f

        if (boxX < 0f) boxX = 0f
        if (boxX + boxW > 1f) boxX = 1f - boxW
        if (boxY < 0f) boxY = 0f
        if (boxY + boxH > 1f) boxY = 1f - boxH

        // 5. Confidence = mean saliency of pixels whose center lies inside the final box.
        val startCol = (boxX * wF).toInt().coerceIn(0, maskWidth - 1)
        val endCol = ((boxX + boxW) * wF).toInt().coerceIn(0, maskWidth)
        val startRow = (boxY * hF).toInt().coerceIn(0, maskHeight - 1)
        val endRow = ((boxY + boxH) * hF).toInt().coerceIn(0, maskHeight)

        var sum = 0f
        var count = 0
        for (row in startRow until endRow) {
            val rowOffset = row * maskWidth
            for (col in startCol until endCol) {
                sum += mask[rowOffset + col]
                count++
            }
        }
        val confidence = if (count > 0) (sum / count).coerceIn(0f, 1f) else 0f

        return CropRegion(
            x = boxX,
            y = boxY,
            width = boxW,
            height = boxH,
            confidence = confidence,
        )
    }
}
