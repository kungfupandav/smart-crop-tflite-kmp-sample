package com.smartcrop.shared.ml

import com.smartcrop.shared.domain.model.CropRegion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CropCalculatorTest {

    private val eps = 0.02f

    /** Assert the region lies fully within the normalized [0, 1] range on all sides. */
    private fun assertWithinBounds(r: CropRegion) {
        assertTrue(r.x >= -eps, "x within bounds: ${r.x}")
        assertTrue(r.y >= -eps, "y within bounds: ${r.y}")
        assertTrue(r.x + r.width <= 1f + eps, "right edge within bounds: ${r.x + r.width}")
        assertTrue(r.y + r.height <= 1f + eps, "bottom edge within bounds: ${r.y + r.height}")
    }

    @Test
    fun allZeroMask_returnsCenter() {
        val mask = FloatArray(100 * 100) { 0f }
        val result = CropCalculator.computeCropRegion(mask, 100, 100, 16f / 9f)
        assertEquals(CropRegion.CENTER, result)
    }

    @Test
    fun emptyArray_returnsCenter() {
        val result = CropCalculator.computeCropRegion(FloatArray(0), 0, 0, 16f / 9f)
        assertEquals(CropRegion.CENTER, result)
    }

    @Test
    fun zeroDimensions_returnsCenter() {
        val mask = FloatArray(100) { 1f }
        assertEquals(CropRegion.CENTER, CropCalculator.computeCropRegion(mask, 0, 10, 1f))
        assertEquals(CropRegion.CENTER, CropCalculator.computeCropRegion(mask, 10, 0, 1f))
        assertEquals(CropRegion.CENTER, CropCalculator.computeCropRegion(mask, -5, 10, 1f))
    }

    @Test
    fun lengthMismatch_returnsCenter() {
        // Declares 100x100 but only provides 50 values.
        val mask = FloatArray(50) { 1f }
        val result = CropCalculator.computeCropRegion(mask, 100, 100, 1f)
        assertEquals(CropRegion.CENTER, result)
    }

    @Test
    fun invalidAspectRatio_returnsCenter() {
        val mask = FloatArray(100 * 100) { 1f }
        assertEquals(CropRegion.CENTER, CropCalculator.computeCropRegion(mask, 100, 100, 0f))
        assertEquals(CropRegion.CENTER, CropCalculator.computeCropRegion(mask, 100, 100, -2f))
    }

    @Test
    fun brightSquareInTopLeftQuadrant_centeredAndMatchesAspectRatio() {
        val w = 100
        val h = 100
        val mask = FloatArray(w * h) { 0f }
        // 20x20 block spanning cols 20..39, rows 20..39 -> center at pixel (30, 30).
        for (row in 20..39) {
            for (col in 20..39) {
                mask[row * w + col] = 1f
            }
        }
        val target = 16f / 9f
        val r = CropCalculator.computeCropRegion(mask, w, h, target)

        // Square mask -> normalized ratio equals pixel ratio.
        assertEquals(target, r.width / r.height, eps)

        // Box center should sit roughly on the salient block center (0.3, 0.3).
        assertEquals(0.3f, r.x + r.width / 2f, eps)
        assertEquals(0.3f, r.y + r.height / 2f, eps)

        assertWithinBounds(r)

        // Block is uniformly bright and fully inside the crop -> high confidence.
        assertTrue(r.confidence > 0f, "confidence should be positive: ${r.confidence}")
    }

    @Test
    fun fullBrightMask_squareTarget_coversWholeImage() {
        val mask = FloatArray(100 * 100) { 1f }
        val r = CropCalculator.computeCropRegion(mask, 100, 100, 1f)

        assertEquals(0f, r.x, eps)
        assertEquals(0f, r.y, eps)
        assertEquals(1f, r.width, eps)
        assertEquals(1f, r.height, eps)
        assertEquals(1f, r.confidence, eps)
        assertWithinBounds(r)
    }

    @Test
    fun expansionOverflowingBounds_staysClamped() {
        val w = 100
        val h = 100
        val mask = FloatArray(w * h) { 0f }
        // 10x10 block hugging the top-left corner -> expansion pushes past the edge.
        for (row in 0..9) {
            for (col in 0..9) {
                mask[row * w + col] = 1f
            }
        }
        val r = CropCalculator.computeCropRegion(mask, w, h, 16f / 9f)

        // Ratio should be preserved (only a shift, no size clamp for this case).
        assertEquals(16f / 9f, r.width / r.height, eps)
        assertWithinBounds(r)
        // Hard bound checks (no epsilon slack) — must never escape the image.
        assertTrue(r.x >= 0f && r.y >= 0f)
        assertTrue(r.x + r.width <= 1f && r.y + r.height <= 1f)
    }

    @Test
    fun extremeWideTarget_clampsToFullWidth() {
        val mask = FloatArray(100 * 100) { 1f }
        // 10:1 target is far wider than a square image can hold.
        val r = CropCalculator.computeCropRegion(mask, 100, 100, 10f)

        assertTrue(r.width <= 1f + eps, "width clamped: ${r.width}")
        assertTrue(r.height <= 1f + eps, "height clamped: ${r.height}")
        assertWithinBounds(r)
    }

    @Test
    fun tinySalientObject_minSizeGuard_enlargesCrop() {
        val w = 100
        val h = 100
        val mask = FloatArray(w * h) { 0f }
        // A 4x4 salient speck near the center -> a very tight crop without a guard.
        for (row in 48..51) {
            for (col in 48..51) {
                mask[row * w + col] = 1f
            }
        }

        val noGuard = CropCalculator.computeCropRegion(mask, w, h, 1f)
        assertTrue(noGuard.width < 0.2f, "unguarded crop is tiny: ${noGuard.width}")

        // With a 0.5 minimum, neither side may fall below half the frame.
        val guarded = CropCalculator.computeCropRegion(mask, w, h, 1f, minSize = 0.5f)
        assertTrue(guarded.width >= 0.5f - eps, "width honors min size: ${guarded.width}")
        assertTrue(guarded.height >= 0.5f - eps, "height honors min size: ${guarded.height}")
        assertEquals(1f, guarded.width / guarded.height, eps) // ratio preserved
        assertWithinBounds(guarded)
    }

    @Test
    fun nonSquareMask_accountsForPixelAspectRatio() {
        // Wide mask (200x100). A full-bright mask with target 2:1 pixel ratio
        // already matches the image, so the whole image is returned undistorted.
        val w = 200
        val h = 100
        val mask = FloatArray(w * h) { 1f }
        val r = CropCalculator.computeCropRegion(mask, w, h, 2f)

        assertEquals(1f, r.width, eps)
        assertEquals(1f, r.height, eps)
        assertWithinBounds(r)
    }
}
