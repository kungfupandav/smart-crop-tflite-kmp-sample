package com.smartcrop.shared.data.repository

import com.smartcrop.shared.data.api.PicsumApi
import com.smartcrop.shared.data.model.PhotoDto
import com.smartcrop.shared.domain.model.Photo
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * Maps Picsum DTOs to [Photo] domain models and drives pagination. Mirrors
 * [CharacterRepository] so the two feeds share the same paging contract:
 * `getPhotos(page)` returns the page plus a `hasMore` flag.
 */
@Inject
@SingleIn(AppScope::class)
class PhotoRepository(
    private val api: PicsumApi
) {

    /**
     * Loads one page of photos. Picsum's list body carries no total-count field
     * (it lives in a `Link` header), so a full page implies more may follow and a
     * short/empty page signals the end of the finite catalog.
     */
    suspend fun getPhotos(page: Int): Pair<List<Photo>, Boolean> {
        val dtos = api.getPhotos(page, PAGE_SIZE)
        val photos = dtos.map { it.toDomain() }
        val hasMore = dtos.size == PAGE_SIZE
        return photos to hasMore
    }

    suspend fun getPhoto(id: String): Photo = api.getPhoto(id).toDomain()

    companion object {
        const val PAGE_SIZE = 30
    }
}

private fun PhotoDto.toDomain() = Photo(
    id = id,
    author = author,
    width = width,
    height = height,
    sourceUrl = url,
)
