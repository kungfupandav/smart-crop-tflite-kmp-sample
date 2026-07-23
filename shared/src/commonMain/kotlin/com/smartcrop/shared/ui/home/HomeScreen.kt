package com.smartcrop.shared.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartcrop.shared.domain.model.CropRegion
import com.smartcrop.shared.ui.LocalAppGraph
import com.smartcrop.shared.ui.components.CharacterCard
import com.smartcrop.shared.ui.theme.NeoButton
import com.smartcrop.shared.ui.theme.NeoColors

private const val PREFETCH_THRESHOLD = 4

@Composable
fun HomeScreen(
    onCharacterClick: (Int) -> Unit,
) {
    val appGraph = LocalAppGraph.current
    val viewModel: HomeViewModel = viewModel {
        HomeViewModel(appGraph.characterRepository, appGraph.cropRegionRepository)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()

    // Infinite scroll: trigger a load when the last visible item is within
    // PREFETCH_THRESHOLD of the end. derivedStateOf keeps this off the layout
    // hot path — the VM guards against concurrent loads / endReached itself.
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisible >= totalItems - 1 - PREFETCH_THRESHOLD
        }
    }

    // When the threshold is crossed, request the next page from a side effect
    // (never during composition). loadNextPage() is idempotent while a request
    // is in flight and no-ops once the end is reached.
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadNextPage()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeoColors.Cream),
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    color = NeoColors.Ink,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            uiState.error != null && uiState.characters.isEmpty() -> {
                ErrorState(
                    message = uiState.error!!,
                    onRetry = viewModel::retry,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            uiState.characters.isEmpty() -> {
                Text(
                    text = "No characters found.",
                    style = MaterialTheme.typography.titleMedium,
                    color = NeoColors.Ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    state = gridState,
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(
                        count = uiState.characters.size,
                        key = { index -> uiState.characters[index].id },
                    ) { index ->
                        val character = uiState.characters[index]
                        CharacterCard(
                            character = character,
                            onClick = { onCharacterClick(character.id) },
                            crop = uiState.crops[character.imageUrl] ?: CropRegion.CENTER,
                        )
                    }

                    if (uiState.isLoadingMore) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(color = NeoColors.Ink)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = NeoColors.Ink,
            textAlign = TextAlign.Center,
        )
        NeoButton(
            text = "Retry",
            backgroundColor = NeoColors.Yellow,
            onClick = onRetry,
        )
    }
}
