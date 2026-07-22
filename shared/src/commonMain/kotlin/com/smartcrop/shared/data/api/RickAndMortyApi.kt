package com.smartcrop.shared.data.api

import com.smartcrop.shared.data.model.CharacterDto
import com.smartcrop.shared.data.model.CharacterResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class RickAndMortyApi {

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    suspend fun getCharacters(page: Int): CharacterResponse {
        return client.get("https://rickandmortyapi.com/api/character") {
            parameter("page", page)
        }.body()
    }

    suspend fun getCharacter(id: Int): CharacterDto {
        return client.get("https://rickandmortyapi.com/api/character/$id").body()
    }
}
