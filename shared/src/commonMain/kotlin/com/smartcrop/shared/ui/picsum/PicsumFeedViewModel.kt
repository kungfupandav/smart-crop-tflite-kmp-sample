package com.smartcrop.shared.ui.picsum

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartcrop.shared.data.repository.PhotoRepository
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

    /** Clears the current error and retries loading the page that failed. */
    fun retry() {
        if (isRequestInFlight) return
        _uiState.update { it.copy(error = null) }
        loadNextPage()
    }
}
