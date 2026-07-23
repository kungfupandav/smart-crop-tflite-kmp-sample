package com.smartcrop.shared.ml

import com.smartcrop.shared.domain.model.CropRegion

/**
 * Platform-specific saliency detection engine.
 *
 * Implementations wrap TFLite on Android and iOS to analyze images
 * and return the most salient region for smart cropping.
 */
expect class SaliencyEngine() {
    /**
     * Analyze raw image bytes and return the optimal crop region.
     *
     * @param imageBytes PNG or JPEG encoded image bytes
     * @param targetAspectRatio desired width/height ratio for the crop (e.g., 16f/9f)
     * @return CropRegion with normalized coordinates, or CropRegion.CENTER on failure
     */
    suspend fun findSalientRegion(
        imageBytes: ByteArray,
        targetAspectRatio: Float = 16f / 9f
    ): CropRegion

    /** Release model resources. */
    fun close()
}
