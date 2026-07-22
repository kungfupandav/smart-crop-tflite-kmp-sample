package com.smartcrop.shared.data.repository

import com.smartcrop.shared.domain.model.CropRegion
import com.smartcrop.shared.ml.SaliencyEngine
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Caching orchestration layer over [SaliencyEngine].
 *
 * Given an image URL, downloads the image bytes over HTTP and runs the shared
 * saliency model to compute the optimal smart-crop [CropRegion]. Each URL is
 * computed at most once and the result is cached in memory for the lifetime of
 * the session (the graph is [SingleIn] [AppScope]).
 *
 * The cache is intentionally in-memory only; Room-backed persistence across
 * sessions is a planned follow-up.
 */
@Inject
@SingleIn(AppScope::class)
class CropRegionRepository(
    private val client: HttpClient,
    private val engine: SaliencyEngine,
) {
    private val cache = mutableMapOf<String, CropRegion>()
    private val mutex = Mutex()

    /**
     * Returns the smart-crop region for [imageUrl], computing it once and caching
     * the result for the session. Falls back to [CropRegion.CENTER] on any failure.
     *
     * @param imageUrl the source image URL; a blank URL yields [CropRegion.CENTER]
     * @param targetAspectRatio desired width/height ratio for the crop
     */
    suspend fun cropFor(imageUrl: String, targetAspectRatio: Float = 1f): CropRegion {
        if (imageUrl.isBlank()) return CropRegion.CENTER

        // Fast path: return a cached result without doing any work.
        mutex.withLock {
            cache[imageUrl]?.let { return it }
        }

        return try {
            // Hold the lock across download + inference so concurrent callers for
            // the same URL de-duplicate onto a single computation.
            mutex.withLock {
                // Re-check: another caller may have populated the cache while we
                // were waiting for the lock.
                cache[imageUrl]?.let { return it }

                val bytes: ByteArray = client.get(imageUrl).body()
                val region = engine.findSalientRegion(bytes, targetAspectRatio)
                cache[imageUrl] = region
                region
            }
        } catch (e: Throwable) {
            // Do not cache failures so a later retry can still succeed.
            CropRegion.CENTER
        }
    }
}
