package com.smartcrop.shared.data.cache

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface CropRegionDao {
    @Upsert
    suspend fun upsert(entity: CropRegionEntity)

    @Query("SELECT * FROM crop_regions WHERE imageUrl = :url")
    suspend fun get(url: String): CropRegionEntity?

    @Query("DELETE FROM crop_regions")
    suspend fun clear()
}
