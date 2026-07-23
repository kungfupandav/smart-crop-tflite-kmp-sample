package com.smartcrop.shared.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.smartcrop.shared.domain.model.Character
import com.smartcrop.shared.ui.LocalAppGraph
import com.smartcrop.shared.ui.sharedImage
import com.smartcrop.shared.ui.theme.NeoBox
import com.smartcrop.shared.ui.theme.NeoButton
import com.smartcrop.shared.ui.theme.NeoColors
import com.smartcrop.shared.ui.theme.NeoPill
import com.smartcrop.shared.ui.theme.NeoStatCard

@Composable
fun DetailScreen(
    characterId: Int,
    onBack: () -> Unit,
) {
    val appGraph = LocalAppGraph.current
    val viewModel: DetailViewModel = viewModel { DetailViewModel(characterId, appGraph.characterRepository) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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

            uiState.error != null -> {
                ErrorState(
                    message = uiState.error!!,
                    onRetry = viewModel::retry,
                    onBack = onBack,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            uiState.character != null -> {
                DetailContent(
                    character = uiState.character!!,
                    onBack = onBack,
                )
            }
        }
    }
}

@Composable
private fun DetailContent(
    character: Character,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        NeoButton(
            text = "← Back",
            backgroundColor = NeoColors.Yellow,
            onClick = onBack,
        )

        // Full, uncropped character portrait.
        NeoBox(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = NeoColors.Cream,
            contentPadding = PaddingValues(10.dp),
        ) {
            AsyncImage(
                model = character.imageUrl,
                contentDescription = character.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .sharedImage("char-${character.id}")
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(10.dp)),
            )
        }

        Text(
            text = character.name,
            style = MaterialTheme.typography.headlineSmall,
            color = NeoColors.Ink,
        )

        NeoPill(
            text = character.status.uppercase(),
            backgroundColor = statusColor(character.status),
        )

        // 2-column grid of metadata stat cards built from weighted rows so it
        // scrolls inside the parent Column (LazyVerticalGrid can't nest here).
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NeoStatCard(
                label = "Species",
                value = character.species.ifBlank { "Unknown" },
                caption = "Species",
                headerColor = NeoColors.OrangeHead,
                bodyColor = NeoColors.OrangeBody,
                modifier = Modifier.weight(1f),
            )
            NeoStatCard(
                label = "Gender",
                value = character.gender.ifBlank { "Unknown" },
                caption = "Gender",
                headerColor = NeoColors.MagentaHead,
                bodyColor = NeoColors.MagentaBody,
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NeoStatCard(
                label = "Origin",
                value = character.originName.ifBlank { "Unknown" },
                caption = "Origin",
                headerColor = NeoColors.CyanHead,
                bodyColor = NeoColors.CyanBody,
                modifier = Modifier.weight(1f),
            )
            NeoStatCard(
                label = "Location",
                value = character.locationName.ifBlank { "Unknown" },
                caption = "Last seen",
                headerColor = NeoColors.LimeHead,
                bodyColor = NeoColors.LimeBody,
                modifier = Modifier.weight(1f),
            )
        }

        NeoStatCard(
            label = "Episodes",
            value = character.episodeCount.toString(),
            caption = "Appearances",
            headerColor = NeoColors.OrangeHead,
            bodyColor = NeoColors.OrangeBody,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
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
        NeoButton(
            text = "← Back",
            backgroundColor = NeoColors.CreamDeep,
            onClick = onBack,
        )
    }
}

private fun statusColor(status: String): Color = when (status.lowercase()) {
    "alive" -> NeoColors.Green
    "dead" -> NeoColors.Coral
    else -> NeoColors.Yellow
}
