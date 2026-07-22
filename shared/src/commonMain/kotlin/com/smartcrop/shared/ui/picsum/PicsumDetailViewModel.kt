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
 * Immutable UI state for the Picsum photo detail screen.
 */
data class PicsumDetailUiState(
    val photo: Photo? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

/**
 * Loads a single photo by id on init. Exposes a [retry] to re-fetch after a
 * failure. Mirrors [com.smartcrop.shared.ui.detail.DetailViewModel].
 */
class PicsumDetailViewModel(
    private val photoId: String,
    private val repository: PhotoRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PicsumDetailUiState())
    val uiState: StateFlow<PicsumDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val photo = repository.getPhoto(photoId)
                _uiState.update {
                    it.copy(photo = photo, isLoading = false, error = null)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Something went wrong")
                }
            }
        }
    }

    /** Clears the current error and re-fetches the photo. */
    fun retry() {
        if (_uiState.value.isLoading) return
        load()
    }
}
