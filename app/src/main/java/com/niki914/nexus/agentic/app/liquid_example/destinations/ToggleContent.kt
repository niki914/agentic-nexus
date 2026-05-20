package com.niki914.nexus.agentic.app.liquid_example.destinations

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.niki914.nexus.agentic.app.liquid_example.BackdropDemoScaffold
import com.niki914.nexus.agentic.app.liquid_example.components.LiquidToggle
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun ToggleContent() {
    val isLightTheme = !isSystemInDarkTheme()
    val backgroundColor =
        if (isLightTheme) Color(0xFFFFFFFF)
        else Color(0xFF121212)

    BackdropDemoScaffold { backdrop ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16f.dp)
        ) {
            var selected by rememberSaveable { mutableStateOf(false) }

            LiquidToggle(
                selected = { selected },
                onSelect = { selected = it },
                backdrop = backdrop,
                modifier = Modifier.padding(horizontal = 32f.dp)
            )

            Box(
                Modifier
                    .padding(24f.dp)
                    .clip(RoundedCornerShape(32f.dp))
                    .background(backgroundColor)
                    .padding(24f.dp)
            ) {
                LiquidToggle(
                    selected = { selected },
                    onSelect = { selected = it },
                    backdrop = rememberCanvasBackdrop { drawRect(backgroundColor) },
                    modifier = Modifier.padding(horizontal = 32f.dp)
                )
            }
        }
    }
}
