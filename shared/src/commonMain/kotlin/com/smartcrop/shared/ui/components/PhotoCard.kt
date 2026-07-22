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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.smartcrop.shared.domain.model.Photo
import com.smartcrop.shared.ui.theme.NeoBox
import com.smartcrop.shared.ui.theme.NeoColors
import com.smartcrop.shared.ui.theme.NeoPill

/**
 * A single photo cell in the Picsum feed grid: a neo-brutalist block with a
 * center-cropped thumbnail (placeholder for the future ML smart-crop), the
 * author, and a pill showing the source dimensions. Mirrors [CharacterCard].
 */
@Composable
fun PhotoCard(
    photo: Photo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            AsyncImage(
                model = photo.thumbnailUrl(),
                contentDescription = "Photo by ${photo.author}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(10.dp)),
            )
            Text(
                text = photo.author,
                style = MaterialTheme.typography.titleSmall,
                color = NeoColors.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            NeoPill(
                text = "${photo.width}×${photo.height}",
                backgroundColor = NeoColors.BluePill,
            )
        }
    }
}
