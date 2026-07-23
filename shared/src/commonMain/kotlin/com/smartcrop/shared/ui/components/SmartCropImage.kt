package com.smartcrop.shared.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import coil3.compose.AsyncImage
import com.smartcrop.shared.domain.model.CropRegion
import com.smartcrop.shared.ui.theme.NeoColors
import kotlin.math.roundToInt

/**
 * Displays [imageUrl] framed on [crop] (a normalized sub-rectangle of the source
 * image) filling this composable's bounds — the smart-crop equivalent of
 * [ContentScale.Crop], but centered on the salient region instead of the image center.
 *
 * The source (whose real aspect ratio is [sourceAspectRatio] = width / height) is
 * scaled *uniformly* so the [crop] sub-rect covers the box, then positioned on the
 * crop's focal point (clamped so the box stays covered). Because the rendered image
 * keeps the source aspect ratio, nothing is ever stretched — a landscape photo shown
 * in a square cell is cropped, not squished. A CENTER crop (0,0,1,1) degrades to a
 * plain center-crop, so this is safe to use before a real crop has been computed.
 */
@Composable
fun SmartCropImage(
    imageUrl: String,
    crop: CropRegion,
    sourceAspectRatio: Float,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val ar = if (sourceAspectRatio.isFinite() && sourceAspectRatio > 0f) sourceAspectRatio else 1f
    // Guard against degenerate crops (never divide by zero / invert).
    val cw = crop.width.coerceIn(0.0001f, 1f)
    val ch = crop.height.coerceIn(0.0001f, 1f)
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.clipToBounds()) {
        val boxW = constraints.maxWidth.toFloat()
        val boxH = constraints.maxHeight.toFloat()

        // Uniform scale so the crop sub-rect just covers the box; dispW×dispH keeps
        // the source aspect ratio, so the rendered image is never stretched.
        val k = maxOf(boxW / (cw * ar), boxH / ch)
        val dispW = ar * k
        val dispH = k
        val focalX = crop.x + cw / 2f
        val focalY = crop.y + ch / 2f
        // Place the focal point at the box center, clamped so the box stays covered.
        val offX = (boxW / 2f - focalX * dispW).coerceIn(boxW - dispW, 0f)
        val offY = (boxH / 2f - focalY * dispH).coerceIn(boxH - dispH, 0f)

        AsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            contentScale = ContentScale.FillBounds, // dispW×dispH already matches source aspect
            modifier = Modifier
                .requiredSize(with(density) { dispW.toDp() }, with(density) { dispH.toDp() })
                .offset { IntOffset(offX.roundToInt(), offY.roundToInt()) },
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
