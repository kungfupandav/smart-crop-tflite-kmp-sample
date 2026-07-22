package com.smartcrop.shared.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CharacterResponse(
    val info: PageInfo,
    val results: List<CharacterDto>
)

@Serializable
data class PageInfo(
    val count: Int,
    val pages: Int,
    val next: String? = null,
    val prev: String? = null
)

@Serializable
data class CharacterDto(
    val id: Int,
    val name: String,
    val status: String,
    val species: String,
    val type: String,
    val gender: String,
    val origin: LocationDto,
    val location: LocationDto,
    val image: String,
    val episode: List<String>,
    val url: String,
    val created: String
)

@Serializable
data class LocationDto(
    val name: String,
    val url: String
)
