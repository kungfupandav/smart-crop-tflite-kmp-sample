package com.smartcrop.shared.ui.detail

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
 * Immutable UI state for the character detail screen.
 */
data class DetailUiState(
    val character: Character? = null,
    /** The smart-crop region for this character (for the detail overlay); null until loaded. */
    val crop: CropRegion? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

/**
 * Loads a single character by id on init. Exposes a [retry] to re-fetch after a
 * failure. Mirrors the pattern of [com.smartcrop.shared.ui.home.HomeViewModel].
 */
class DetailViewModel(
    private val characterId: Int,
    private val repository: CharacterRepository,
    private val cropRegionRepository: CropRegionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val character = repository.getCharacter(characterId)
                _uiState.update {
                    it.copy(character = character, isLoading = false, error = null)
                }
                // Cache hit from the feed's inference; drives the crop overlay.
                val region = cropRegionRepository.cropFor(character.imageUrl, 1f)
                _uiState.update { it.copy(crop = region) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Something went wrong")
                }
            }
        }
    }

    /** Clears the current error and re-fetches the character. */
    fun retry() {
        if (_uiState.value.isLoading) return
        load()
    }
}
