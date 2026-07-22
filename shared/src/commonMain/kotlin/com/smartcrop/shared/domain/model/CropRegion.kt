package com.smartcrop.shared.domain.model

/**
 * Represents a crop region within an image, using normalized coordinates (0.0 to 1.0).
 *
 * @param x Left edge of the crop box (0.0 = left, 1.0 = right)
 * @param y Top edge of the crop box (0.0 = top, 1.0 = bottom)
 * @param width Width of the crop box as a fraction of image width
 * @param height Height of the crop box as a fraction of image height
 * @param confidence How confident the model is in this region (0.0 to 1.0)
 */
data class CropRegion(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val confidence: Float = 1.0f
) {
    companion object {
        /** Fallback: center crop covering the full image. */
        val CENTER = CropRegion(0f, 0f, 1f, 1f, 0f)
    }
}
