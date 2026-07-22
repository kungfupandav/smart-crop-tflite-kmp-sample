package com.smartcrop.shared.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.smartcrop.shared.domain.model.Character
import com.smartcrop.shared.ui.theme.NeoColors
import com.smartcrop.shared.ui.theme.NeoBox
import com.smartcrop.shared.ui.theme.NeoPill

/**
 * A single character cell in the feed grid: a neo-brutalist block with a
 * center-cropped image (placeholder for the future ML smart-crop), the name,
 * and a status pill colored by life state.
 */
@Composable
fun CharacterCard(
    character: Character,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NeoBox(
        modifier = modifier,
        backgroundColor = NeoColors.Cream,
        onClick = onClick,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AsyncImage(
                model = character.imageUrl,
                contentDescription = character.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(10.dp)),
            )
            Text(
                text = character.name,
                style = MaterialTheme.typography.titleSmall,
                color = NeoColors.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            NeoPill(
                text = character.status.uppercase(),
                backgroundColor = statusColor(character.status),
            )
        }
    }
}

private fun statusColor(status: String) = when (status.lowercase()) {
    "alive" -> NeoColors.Green
    "dead" -> NeoColors.Coral
    else -> NeoColors.Yellow
}
