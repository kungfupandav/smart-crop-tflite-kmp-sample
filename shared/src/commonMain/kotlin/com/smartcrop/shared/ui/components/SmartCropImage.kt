package com.smartcrop.shared.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.smartcrop.shared.domain.model.CropRegion
import com.smartcrop.shared.ui.theme.NeoColors

/**
 * Displays [imageUrl] cropped to [crop] (a normalized sub-rectangle of the source
 * image), scaled to fill this composable's bounds — the smart-crop equivalent of
 * [ContentScale.Crop], but framed on the salient region instead of the image center.
 *
 * How it works: the full image is stretched to fill the box ([ContentScale.FillBounds]),
 * then a graphics-layer transform (pivoted top-left) scales the crop sub-rect up to
 * fill the box and offsets it into place. The FillBounds distortion is exactly
 * cancelled by the scale whenever the crop's *pixel* aspect matches the box aspect —
 * which `CropCalculator` guarantees by taking the display box aspect as its target —
 * so the visible result is undistorted. A CENTER crop (0,0,1,1) renders the whole
 * image, so this is safe to use before a real crop has been computed.
 */
@Composable
fun SmartCropImage(
    imageUrl: String,
    crop: CropRegion,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    // Guard against degenerate crops (never divide by zero / invert).
    val cw = crop.width.coerceIn(0.0001f, 1f)
    val ch = crop.height.coerceIn(0.0001f, 1f)

    BoxWithConstraints(modifier = modifier.clipToBounds()) {
        val boxW = constraints.maxWidth.toFloat()
        val boxH = constraints.maxHeight.toFloat()
        AsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = 1f / cw
                    scaleY = 1f / ch
                    translationX = -crop.x * boxW / cw
                    translationY = -crop.y * boxH / ch
                },
        )
    }
}

/**
 * Draws the outline of [crop] (normalized coordinates) over the composable's bounds —
 * used on the detail screen to visualize the smart-crop region the feed chose. Assumes
 * this overlay is sized to exactly match the displayed (Fit) image.
 */
@Composable
fun CropRegionOverlay(crop: CropRegion, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRect(
            color = NeoColors.Coral,
            topLeft = Offset(crop.x * size.width, crop.y * size.height),
            size = Size(crop.width * size.width, crop.height * size.height),
            style = Stroke(width = 6f),
        )
    }
}
