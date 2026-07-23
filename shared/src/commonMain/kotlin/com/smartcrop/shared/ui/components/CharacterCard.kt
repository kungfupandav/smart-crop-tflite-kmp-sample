package com.smartcrop.shared.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.smartcrop.shared.domain.model.Character
import com.smartcrop.shared.domain.model.CropRegion
import com.smartcrop.shared.ui.sharedImage
import com.smartcrop.shared.ui.theme.NeoBox
import com.smartcrop.shared.ui.theme.NeoColors
import com.smartcrop.shared.ui.theme.NeoPill

/**
 * A single character cell in the feed grid: a neo-brutalist block whose image is
 * smart-cropped to [crop] (the salient region computed on-device), the name, and a
 * status pill colored by life state.
 *
 * [crop] defaults to [CropRegion.CENTER] (full image) so the cell renders sensibly
 * before inference completes, then re-crops itself once the region arrives.
 */
@Composable
fun CharacterCard(
    character: Character,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    crop: CropRegion = CropRegion.CENTER,
) {
    // Rick & Morty avatars are square (300×300); the cell takes the crop's own
    // aspect ratio (full width, dynamic height) so the whole crop shows undistorted.
    val cellAspect = if (crop.height > 0f) crop.width / crop.height else 1f
    NeoBox(
        modifier = modifier,
        backgroundColor = NeoColors.Cream,
        onClick = onClick,
        contentPadding = PaddingValues(10.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SmartCropImage(
                imageUrl = character.imageUrl,
                crop = crop,
                contentDescription = character.name,
                modifier = Modifier
                    .sharedImage("char-${character.id}")
                    .fillMaxWidth()
                    .aspectRatio(cellAspect)
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
