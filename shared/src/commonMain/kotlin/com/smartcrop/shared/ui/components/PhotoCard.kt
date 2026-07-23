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
import com.smartcrop.shared.domain.model.CropRegion
import com.smartcrop.shared.domain.model.Photo
import com.smartcrop.shared.ui.sharedImage
import com.smartcrop.shared.ui.theme.NeoBox
import com.smartcrop.shared.ui.theme.NeoColors
import com.smartcrop.shared.ui.theme.NeoPill

/**
 * A single photo cell in the Picsum feed grid: a neo-brutalist block whose image
 * is smart-cropped to [crop] (the salient region computed on-device), the author,
 * and a pill showing the source dimensions. Mirrors [CharacterCard].
 *
 * [crop] defaults to [CropRegion.CENTER] (full image) so the cell renders sensibly
 * before inference completes, then re-crops itself once the region arrives.
 */
@Composable
fun PhotoCard(
    photo: Photo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    crop: CropRegion = CropRegion.CENTER,
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
            SmartCropImage(
                imageUrl = photo.detailUrl(),
                crop = crop,
                sourceAspectRatio = photo.width.toFloat() / photo.height,
                contentDescription = "Photo by ${photo.author}",
                modifier = Modifier
                    .sharedImage("photo-${photo.id}")
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
