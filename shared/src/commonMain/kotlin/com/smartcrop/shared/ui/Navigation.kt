package com.smartcrop.shared.ui

import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
data class DetailRoute(val characterId: Int)
