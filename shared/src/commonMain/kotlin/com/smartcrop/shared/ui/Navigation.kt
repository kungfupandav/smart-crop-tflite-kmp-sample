package com.smartcrop.shared.ui

import kotlinx.serialization.Serializable

/** Landing screen: choose which feed to browse. The app's start destination. */
@Serializable
object EntryRoute

// --- Rick & Morty flow ---

@Serializable
object HomeRoute

@Serializable
data class DetailRoute(val characterId: Int)

// --- Picsum flow ---

@Serializable
object PicsumFeedRoute

@Serializable
data class PicsumDetailRoute(val photoId: String)
