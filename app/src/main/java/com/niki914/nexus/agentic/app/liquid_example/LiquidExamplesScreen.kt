package com.niki914.nexus.agentic.app.liquid_example

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.niki914.nexus.agentic.app.liquid_example.destinations.AdaptiveLuminanceGlassContent
import com.niki914.nexus.agentic.app.liquid_example.destinations.BottomTabsContent
import com.niki914.nexus.agentic.app.liquid_example.destinations.ButtonsContent
import com.niki914.nexus.agentic.app.liquid_example.destinations.DialogContent
import com.niki914.nexus.agentic.app.liquid_example.destinations.LazyScrollContainerContent
import com.niki914.nexus.agentic.app.liquid_example.destinations.LibrepodsStyleExamples
import com.niki914.nexus.agentic.app.liquid_example.destinations.ScrollContainerContent
import com.niki914.nexus.agentic.app.liquid_example.destinations.SliderContent
import com.niki914.nexus.agentic.app.liquid_example.destinations.ToggleContent

@Composable
fun LiquidExamplesScreen() {
    val destinations = listOf<@Composable () -> Unit>(
        { DialogContent() },
        { ButtonsContent() },
        { SliderContent() },
        { ToggleContent() },
        { BottomTabsContent() },
        { LibrepodsStyleExamples() },
        { AdaptiveLuminanceGlassContent() },
        { ScrollContainerContent() },
        { LazyScrollContainerContent() }
    )

    var currentScreenIndex by remember { mutableIntStateOf(5) } // Modify this to test other screens

    BackHandler {
        currentScreenIndex = (currentScreenIndex + 1) % destinations.size
    }

    if (currentScreenIndex in destinations.indices) {
        destinations[currentScreenIndex]()
    }
}
