package com.smartcrop.shared.data.cache

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

@Database(entities = [CropRegionEntity::class], version = 1)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cropRegionDao(): CropRegionDao
}

// The Room KSP compiler generates the `actual` object for each platform.
// Do not provide an `actual` implementation manually.
@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

/**
 * Finalizes a platform-provided [RoomDatabase.Builder] with the bundled SQLite
 * driver and an IO coroutine context, then builds the database.
 */
fun buildDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase =
    builder
        .setDriver(BundledSQLiteDriver())
        // Dispatchers.IO is JVM-only (still internal on Kotlin/Native as of
        // kotlinx-coroutines 1.10.2), so use Default for KMP compatibility.
        .setQueryCoroutineContext(Dispatchers.Default)
        .build()
