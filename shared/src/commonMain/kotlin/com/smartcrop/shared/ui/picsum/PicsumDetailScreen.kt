package com.smartcrop.shared.ui.picsum

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.smartcrop.shared.domain.model.Photo
import com.smartcrop.shared.ui.LocalAppGraph
import com.smartcrop.shared.ui.theme.NeoBox
import com.smartcrop.shared.ui.theme.NeoButton
import com.smartcrop.shared.ui.theme.NeoColors
import com.smartcrop.shared.ui.theme.NeoPill
import com.smartcrop.shared.ui.theme.NeoStatCard
import kotlin.math.roundToInt

@Composable
fun PicsumDetailScreen(
    photoId: String,
    onBack: () -> Unit,
) {
    val appGraph = LocalAppGraph.current
    val viewModel: PicsumDetailViewModel = viewModel {
        PicsumDetailViewModel(photoId, appGraph.photoRepository)
    }
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

            uiState.photo != null -> {
                DetailContent(
                    photo = uiState.photo!!,
                    onBack = onBack,
                )
            }
        }
    }
}

@Composable
private fun DetailContent(
    photo: Photo,
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

        // Full, uncropped photo at its native aspect ratio.
        NeoBox(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = NeoColors.Cream,
            contentPadding = PaddingValues(10.dp),
        ) {
            AsyncImage(
                model = photo.detailUrl(),
                contentDescription = "Photo by ${photo.author}",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(photo.width.toFloat() / photo.height)
                    .clip(RoundedCornerShape(10.dp)),
            )
        }

        Text(
            text = photo.author,
            style = MaterialTheme.typography.headlineSmall,
            color = NeoColors.Ink,
        )

        NeoPill(
            text = "PICSUM #${photo.id}",
            backgroundColor = NeoColors.BluePill,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NeoStatCard(
                label = "Width",
                value = "${photo.width}",
                caption = "Pixels",
                headerColor = NeoColors.OrangeHead,
                bodyColor = NeoColors.OrangeBody,
                modifier = Modifier.weight(1f),
            )
            NeoStatCard(
                label = "Height",
                value = "${photo.height}",
                caption = "Pixels",
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
                label = "Aspect",
                value = aspectRatioLabel(photo.width, photo.height),
                caption = "Width ÷ height",
                headerColor = NeoColors.CyanHead,
                bodyColor = NeoColors.CyanBody,
                modifier = Modifier.weight(1f),
            )
            NeoStatCard(
                label = "ID",
                value = photo.id,
                caption = "Picsum",
                headerColor = NeoColors.LimeHead,
                bodyColor = NeoColors.LimeBody,
                modifier = Modifier.weight(1f),
            )
        }

        NeoStatCard(
            label = "Source",
            value = sourceHost(photo.sourceUrl),
            caption = "Original photographer",
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

/** e.g. 1.50 for a 3:2 photo. Rounded to two decimals; common-safe (no String.format). */
private fun aspectRatioLabel(width: Int, height: Int): String {
    if (height <= 0) return "—"
    val hundredths = (width.toDouble() / height * 100).roundToInt()
    val whole = hundredths / 100
    val frac = (hundredths % 100).toString().padStart(2, '0')
    return "$whole.$frac"
}

/** "https://unsplash.com/photos/x" -> "unsplash.com". */
private fun sourceHost(url: String): String =
    url.substringAfter("://").substringBefore("/").ifBlank { url }
