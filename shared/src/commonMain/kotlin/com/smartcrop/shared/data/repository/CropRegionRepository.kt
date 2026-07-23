package com.smartcrop.shared.data.repository

import com.smartcrop.shared.domain.model.CropRegion
import com.smartcrop.shared.ml.SaliencyEngine
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.CompletableDeferred
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
    private val inFlight = mutableMapOf<String, CompletableDeferred<CropRegion>>()
    private val mutex = Mutex()

    /**
     * Returns the smart-crop region for [imageUrl], computing it exactly once per URL
     * and caching the result for the session. Concurrent callers for the same URL
     * (e.g. a feed cell and the detail overlay) join a single in-flight computation,
     * so they always receive the *identical* crop — the model never runs twice for one
     * image and can't hand two views slightly different (or CENTER-vs-real) results.
     * Falls back to [CropRegion.CENTER] on failure.
     *
     * @param imageUrl the source image URL; a blank URL yields [CropRegion.CENTER]
     * @param targetAspectRatio desired width/height ratio for the crop
     */
    suspend fun cropFor(imageUrl: String, targetAspectRatio: Float = 1f): CropRegion {
        if (imageUrl.isBlank()) return CropRegion.CENTER

        // Under a short lock: return the cached crop, join an in-flight computation,
        // or claim ownership of a new one. Only the download + inference runs unlocked.
        var join: CompletableDeferred<CropRegion>? = null
        var own: CompletableDeferred<CropRegion>? = null
        mutex.withLock {
            cache[imageUrl]?.let { return it }
            val pending = inFlight[imageUrl]
            if (pending != null) {
                join = pending
            } else {
                own = CompletableDeferred<CropRegion>().also { inFlight[imageUrl] = it }
            }
        }

        join?.let { return it.await() }

        val deferred = own!!
        val region = try {
            val bytes: ByteArray = client.get(imageUrl).body()
            engine.findSalientRegion(bytes, targetAspectRatio)
        } catch (e: Throwable) {
            CropRegion.CENTER
        }
        mutex.withLock {
            cache[imageUrl] = region
            inFlight.remove(imageUrl)
        }
        deferred.complete(region) // unblock any joiners with the same result
        return region
    }
}
