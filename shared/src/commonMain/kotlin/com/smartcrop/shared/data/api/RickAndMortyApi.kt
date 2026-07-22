package com.smartcrop.shared.data.api

import com.smartcrop.shared.data.model.CharacterDto
import com.smartcrop.shared.data.model.CharacterResponse
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

/**
 * Thin Ktor wrapper over the Rick & Morty REST API. The [HttpClient] is supplied
 * by the DI graph (see `di/AppGraph`) rather than constructed here, so the client
 * (and its connection pool) is shared as an application-scoped singleton.
 */
@Inject
@SingleIn(AppScope::class)
class RickAndMortyApi(private val client: HttpClient) {

    suspend fun getCharacters(page: Int): CharacterResponse {
        return client.get("https://rickandmortyapi.com/api/character") {
            parameter("page", page)
        }.body()
    }

    suspend fun getCharacter(id: Int): CharacterDto {
        return client.get("https://rickandmortyapi.com/api/character/$id").body()
    }
}
