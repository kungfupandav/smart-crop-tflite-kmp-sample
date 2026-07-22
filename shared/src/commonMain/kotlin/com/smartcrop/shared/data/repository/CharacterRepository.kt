package com.smartcrop.shared.data.repository

import com.smartcrop.shared.data.api.RickAndMortyApi
import com.smartcrop.shared.data.model.CharacterDto
import com.smartcrop.shared.domain.model.Character
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(AppScope::class)
class CharacterRepository(
    private val api: RickAndMortyApi
) {

    suspend fun getCharacters(page: Int): Pair<List<Character>, Boolean> {
        val response = api.getCharacters(page)
        val characters = response.results.map { it.toDomain() }
        val hasMore = response.info.next != null
        return characters to hasMore
    }

    suspend fun getCharacter(id: Int): Character {
        return api.getCharacter(id).toDomain()
    }
}

private fun CharacterDto.toDomain() = Character(
    id = id,
    name = name,
    status = status,
    species = species,
    type = type,
    gender = gender,
    originName = origin.name,
    locationName = location.name,
    imageUrl = image,
    episodeCount = episode.size
)
