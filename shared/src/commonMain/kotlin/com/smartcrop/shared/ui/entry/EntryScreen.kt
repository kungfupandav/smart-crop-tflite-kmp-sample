package com.smartcrop.shared.ui.entry

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.smartcrop.shared.ui.theme.NeoBox
import com.smartcrop.shared.ui.theme.NeoColors
import com.smartcrop.shared.ui.theme.NeoPill

/**
 * The app's landing screen: pick which paginated feed to browse. Each choice
 * routes into a feed → detail flow that shares the same paging + neo-brutalist
 * design, differing only in data source (Rick & Morty vs. Picsum).
 */
@Composable
fun EntryScreen(
    onRickAndMorty: () -> Unit,
    onPicsum: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeoColors.Cream)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(24.dp))

        Text(
            text = "SMART CROP",
            style = MaterialTheme.typography.displaySmall,
            color = NeoColors.Ink,
        )
        NeoPill(text = "CHOOSE A FEED", backgroundColor = NeoColors.Yellow)

        Spacer(Modifier.height(8.dp))

        FeedChoiceCard(
            title = "Rick & Morty",
            caption = "Characters · paginated feed → detail",
            backgroundColor = NeoColors.Green,
            onClick = onRickAndMorty,
        )
        FeedChoiceCard(
            title = "Picsum Photos",
            caption = "Photographs · Picsum paginated API → detail",
            backgroundColor = NeoColors.BluePill,
            onClick = onPicsum,
        )
    }
}

@Composable
private fun FeedChoiceCard(
    title: String,
    caption: String,
    backgroundColor: Color,
    onClick: () -> Unit,
) {
    NeoBox(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = backgroundColor,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = NeoColors.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = caption,
                style = MaterialTheme.typography.bodyMedium,
                color = NeoColors.Ink,
            )
        }
    }
}
