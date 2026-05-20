package com.niki914.nexus.agentic.app.liquid_example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

@Composable
fun BackdropDemoScaffold(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(backdrop: LayerBackdrop) -> Unit
) {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val backdrop = rememberLayerBackdrop()

        Box(
            Modifier
                .layerBackdrop(backdrop)
                .then(modifier)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )

        content(backdrop)
    }
}
