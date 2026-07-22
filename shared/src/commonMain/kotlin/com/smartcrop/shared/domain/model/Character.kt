package com.smartcrop.shared.domain.model

/**
 * Domain model for a Rick & Morty character.
 */
data class Character(
    val id: Int,
    val name: String,
    val status: String,
    val species: String,
    val type: String,
    val gender: String,
    val originName: String,
    val locationName: String,
    val imageUrl: String,
    val episodeCount: Int
)
