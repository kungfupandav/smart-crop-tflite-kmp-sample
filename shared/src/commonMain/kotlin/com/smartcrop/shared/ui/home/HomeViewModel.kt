package com.smartcrop.shared.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartcrop.shared.data.repository.CharacterRepository
import com.smartcrop.shared.data.repository.CropRegionRepository
import com.smartcrop.shared.domain.model.Character
import com.smartcrop.shared.domain.model.CropRegion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Immutable UI state for the character feed.
 */
data class HomeUiState(
    val characters: List<Character> = emptyList(),
    /** Smart-crop region per character image URL; absent until inference completes. */
    val crops: Map<String, CropRegion> = emptyMap(),
    val isLoading: Boolean = false,      // first-page load
    val isLoadingMore: Boolean = false,  // subsequent pages
    val endReached: Boolean = false,
    val error: String? = null,
)

/**
 * Holds the paging state for the home feed. Loads page 1 on init and appends
 * further pages as [loadNextPage] is called from the scroll observer.
 */
class HomeViewModel(
    private val repository: CharacterRepository,
    private val cropRegionRepository: CropRegionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // The page currently being requested (or last successfully loaded).
    private var currentPage = 1
    private var isRequestInFlight = false

    init {
        loadNextPage()
    }

    /**
     * Loads the next page of characters. Guards against concurrent requests and
     * against loading past the end of the list. Distinguishes the first-page
     * load ([HomeUiState.isLoading]) from subsequent loads
     * ([HomeUiState.isLoadingMore]).
     */
    fun loadNextPage() {
        val state = _uiState.value
        if (isRequestInFlight || state.endReached) return

        val isFirstPage = state.characters.isEmpty()
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
                val (newCharacters, hasMore) = repository.getCharacters(currentPage)
                _uiState.update {
                    it.copy(
                        characters = it.characters + newCharacters,
                        isLoading = false,
                        isLoadingMore = false,
                        endReached = !hasMore,
                        error = null,
                    )
                }
                currentPage += 1
                computeCrops(newCharacters)
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
     * Runs saliency inference for each newly-loaded character in the background and
     * folds the resulting crop into [HomeUiState.crops] as it arrives, so cells
     * re-crop themselves once ready. Results are memoized by the repository.
     */
    private fun computeCrops(characters: List<Character>) {
        for (character in characters) {
            val url = character.imageUrl
            viewModelScope.launch {
                val region = cropRegionRepository.cropFor(url, FEED_ASPECT)
                _uiState.update { it.copy(crops = it.crops + (url to region)) }
            }
        }
    }

    /**
     * Clears the current error and retries loading the page that failed.
     */
    fun retry() {
        if (isRequestInFlight) return
        _uiState.update { it.copy(error = null) }
        loadNextPage()
    }

    private companion object {
        /** Feed cells are square, so smart-crop targets a 1:1 region. */
        const val FEED_ASPECT = 1f
    }
}
