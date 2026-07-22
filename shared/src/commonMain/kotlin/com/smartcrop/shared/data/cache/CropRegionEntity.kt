package com.smartcrop.shared.data.cache

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity that caches a computed crop region for a given image URL.
 * The image URL is the cache key.
 */
@Entity(tableName = "crop_regions")
data class CropRegionEntity(
    @PrimaryKey val imageUrl: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val confidence: Float
)
