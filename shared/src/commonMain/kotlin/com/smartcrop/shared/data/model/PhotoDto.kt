package com.smartcrop.shared.data.model

import kotlinx.serialization.Serializable

/**
 * A single item from the Picsum `GET /v2/list` array and the
 * `GET /id/{id}/info` object — both return the same shape.
 *
 * `download_url` (the original full-res file) is intentionally omitted; the app
 * builds its own correctly-sized URLs from `id` (see the `Photo` domain model).
 */
@Serializable
data class PhotoDto(
    val id: String,
    val author: String,
    val width: Int,
    val height: Int,
    val url: String,
)
