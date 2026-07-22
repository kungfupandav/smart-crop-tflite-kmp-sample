package com.smartcrop.shared.ui.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun DetailScreen(
    characterId: Int,
    onBack: () -> Unit
) {
    // TODO: Full image, metadata, saliency overlay toggle
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Detail — Character #$characterId")
    }
}
