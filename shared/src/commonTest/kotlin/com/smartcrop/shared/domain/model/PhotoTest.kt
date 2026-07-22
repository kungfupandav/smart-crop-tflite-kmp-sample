package com.smartcrop.shared.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class PhotoTest {

    private fun photo(id: String = "42", w: Int = 5000, h: Int = 3333) =
        Photo(id = id, author = "Ada", width = w, height = h, sourceUrl = "https://unsplash.com/photos/x")

    @Test
    fun thumbnailUrl_isSquareAtRequestedSize() {
        assertEquals("https://picsum.photos/id/42/400/400", photo().thumbnailUrl())
        assertEquals("https://picsum.photos/id/42/200/200", photo().thumbnailUrl(200))
    }

    @Test
    fun detailUrl_landscape_scalesLongEdgeToMax() {
        // 5000x3333 -> longest edge (width) becomes 600, height scaled proportionally.
        assertEquals("https://picsum.photos/id/42/600/399", photo(w = 5000, h = 3333).detailUrl())
    }

    @Test
    fun detailUrl_portrait_scalesLongEdgeToMax() {
        // 3000x4000 -> longest edge (height) becomes 600, width scaled proportionally.
        assertEquals("https://picsum.photos/id/42/450/600", photo(w = 3000, h = 4000).detailUrl())
    }

    @Test
    fun detailUrl_square_bothEdgesEqualMax() {
        assertEquals("https://picsum.photos/id/42/600/600", photo(w = 1000, h = 1000).detailUrl())
    }

    @Test
    fun scaledTo_neverProducesZeroForExtremeAspect() {
        // A very wide image must still yield at least 1px on the short edge.
        val (w, h) = photo(w = 10000, h = 1).scaledTo(600)
        assertEquals(600, w)
        assertEquals(1, h)
    }

    @Test
    fun scaledTo_invalidDimensions_fallBackToSquare() {
        assertEquals(600 to 600, photo(w = 0, h = 0).scaledTo(600))
    }
}
