package com.smartcrop.shared.data.api

import com.smartcrop.shared.data.model.PhotoDto
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

/**
 * Thin Ktor wrapper over the Picsum API. The [HttpClient] is supplied by the DI
 * graph (see `di/AppGraph`), so it shares the same application-scoped client and
 * connection pool as [RickAndMortyApi].
 *
 * Docs: https://picsum.photos — the paginated list lives at `/v2/list`, and
 * per-photo metadata at `/id/{id}/info`.
 */
@Inject
@SingleIn(AppScope::class)
class PicsumApi(private val client: HttpClient) {

    suspend fun getPhotos(page: Int, limit: Int): List<PhotoDto> {
        return client.get("https://picsum.photos/v2/list") {
            parameter("page", page)
            parameter("limit", limit)
        }.body()
    }

    suspend fun getPhoto(id: String): PhotoDto {
        return client.get("https://picsum.photos/id/$id/info").body()
    }
}
