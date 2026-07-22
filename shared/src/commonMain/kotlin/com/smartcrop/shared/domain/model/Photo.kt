package com.smartcrop.shared.domain.model

/**
 * Domain model for a Picsum photograph.
 *
 * Picsum serves any requested dimensions from `/{BASE}/id/{id}/{w}/{h}`, so image
 * URLs are derived on demand from [id] rather than stored — the API's own
 * `download_url` points at the multi-thousand-pixel original, too heavy for a
 * feed cell.
 */
data class Photo(
    val id: String,
    val author: String,
    val width: Int,
    val height: Int,
    /** The Unsplash source page for attribution. */
    val sourceUrl: String,
) {
    /** Square, center-cropped image for the feed grid. */
    fun thumbnailUrl(size: Int = 400): String = "$BASE/id/$id/$size/$size"

    /**
     * Aspect-preserving image scaled so the longest edge is [maxEdge] px —
     * used on the detail screen so the full photo shows without distortion.
     */
    fun detailUrl(maxEdge: Int = 600): String {
        val (w, h) = scaledTo(maxEdge)
        return "$BASE/id/$id/$w/$h"
    }

    /** Scales [width]×[height] so the longest edge equals [maxEdge] (min 1 px). */
    internal fun scaledTo(maxEdge: Int): Pair<Int, Int> {
        if (width <= 0 || height <= 0) return maxEdge to maxEdge
        return if (width >= height) {
            maxEdge to maxOf(1, (maxEdge.toLong() * height / width).toInt())
        } else {
            maxOf(1, (maxEdge.toLong() * width / height).toInt()) to maxEdge
        }
    }

    companion object {
        const val BASE = "https://picsum.photos"
    }
}
