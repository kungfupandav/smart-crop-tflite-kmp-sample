package com.smartcrop.shared.ui.picsum

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartcrop.shared.data.repository.CropRegionRepository
import com.smartcrop.shared.data.repository.PhotoRepository
import com.smartcrop.shared.domain.model.CropRegion
import com.smartcrop.shared.domain.model.Photo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Immutable UI state for the Picsum photo feed.
 */
data class PicsumFeedUiState(
    val photos: List<Photo> = emptyList(),
    /** Smart-crop region per feed image URL; absent until inference completes. */
    val crops: Map<String, CropRegion> = emptyMap(),
    val isLoading: Boolean = false,      // first-page load
    val isLoadingMore: Boolean = false,  // subsequent pages
    val endReached: Boolean = false,
    val error: String? = null,
)

/**
 * Paging state for the Picsum feed. Loads page 1 on init and appends further
 * pages as [loadNextPage] is called from the scroll observer. Mirrors
 * [com.smartcrop.shared.ui.home.HomeViewModel].
 */
class PicsumFeedViewModel(
    private val repository: PhotoRepository,
    private val cropRegionRepository: CropRegionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PicsumFeedUiState())
    val uiState: StateFlow<PicsumFeedUiState> = _uiState.asStateFlow()

    // The page currently being requested (or last successfully loaded).
    private var currentPage = 1
    private var isRequestInFlight = false

    init {
        loadNextPage()
    }

    /**
     * Loads the next page of photos. Guards against concurrent requests and
     * against loading past the end of the catalog. Distinguishes the first-page
     * load ([PicsumFeedUiState.isLoading]) from subsequent loads
     * ([PicsumFeedUiState.isLoadingMore]).
     */
    fun loadNextPage() {
        val state = _uiState.value
        if (isRequestInFlight || state.endReached) return

        val isFirstPage = state.photos.isEmpty()
        isRequestInFlight = true
        _uiState.update {
            it.copy(
                isLoading = isFirstPage,
                isLoadingMore = !isFirstPage,
                error = null,
            )
        }

        viewModelScope.launch {
            try {
                val (newPhotos, hasMore) = repository.getPhotos(currentPage)
                _uiState.update {
                    it.copy(
                        photos = it.photos + newPhotos,
                        isLoading = false,
                        isLoadingMore = false,
                        endReached = !hasMore,
                        error = null,
                    )
                }
                currentPage += 1
                computeCrops(newPhotos)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        error = e.message ?: "Something went wrong",
                    )
                }
            } finally {
                isRequestInFlight = false
            }
        }
    }

    /**
     * Runs saliency inference for each newly-loaded photo in the background and
     * folds the resulting crop into [PicsumFeedUiState.crops] as it arrives, so
     * cells re-crop themselves once ready. Results are memoized by the repository.
     */
    private fun computeCrops(photos: List<Photo>) {
        for (photo in photos) {
            val url = photo.detailUrl()
            viewModelScope.launch {
                // Target a 1:1 region in the (square) saliency-mask space; on the
                // real source that maps to a rectangle with the source's own aspect
                // ratio, which the feed cell then shows in full without stretching.
                val region = cropRegionRepository.cropFor(url, SOURCE_ASPECT_TARGET)
                _uiState.update { it.copy(crops = it.crops + (url to region)) }
            }
        }
    }

    /** Clears the current error and retries loading the page that failed. */
    fun retry() {
        if (isRequestInFlight) return
        _uiState.update { it.copy(error = null) }
        loadNextPage()
    }

    private companion object {
        /**
         * The saliency mask is square (the source resized to 320²), so a 1:1 target
         * yields a normalized-square crop — which on the non-square source is a
         * rectangle with the source's own aspect ratio.
         */
        const val SOURCE_ASPECT_TARGET = 1f
    }
}
