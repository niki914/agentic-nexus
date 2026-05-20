package com.niki914.nexus.agentic.app.liquid_example

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.niki914.nexus.agentic.app.liquid_example.components.LiquidExampleChrome
import com.niki914.nexus.agentic.app.liquid_example.destinations.AdaptiveLuminanceGlassContent
import com.niki914.nexus.agentic.app.liquid_example.destinations.BottomTabsContent
import com.niki914.nexus.agentic.app.liquid_example.destinations.ButtonsContent
import com.niki914.nexus.agentic.app.liquid_example.destinations.DialogContent
import com.niki914.nexus.agentic.app.liquid_example.destinations.LazyScrollContainerContent
import com.niki914.nexus.agentic.app.liquid_example.destinations.LibrepodsStyleExamples
import com.niki914.nexus.agentic.app.liquid_example.destinations.ScrollContainerContent
import com.niki914.nexus.agentic.app.liquid_example.destinations.SliderContent
import com.niki914.nexus.agentic.app.liquid_example.destinations.ToggleContent

private data class LiquidDestination(
    val title: String,
    val content: @Composable () -> Unit
)

@Composable
fun LiquidExamplesScreen() {
    val destinations = listOf(
        LiquidDestination("Dialog") { DialogContent() },
        LiquidDestination("Buttons") { ButtonsContent() },
        LiquidDestination("Slider") { SliderContent() },
        LiquidDestination("Toggle") { ToggleContent() },
        LiquidDestination("Bottom Tabs") { BottomTabsContent() },
        LiquidDestination("Librepods Style Examples") { LibrepodsStyleExamples() },
        LiquidDestination("Adaptive Luminance Glass") { AdaptiveLuminanceGlassContent() },
        LiquidDestination("Scroll Container") { ScrollContainerContent() },
        LiquidDestination("Lazy Scroll Container") { LazyScrollContainerContent() }
    )

    var currentScreenIndex by remember { mutableIntStateOf(5) } // Modify this to test other screens

    val goNext = {
        currentScreenIndex = (currentScreenIndex + 1) % destinations.size
    }
    val goPrev = {
        currentScreenIndex = (currentScreenIndex - 1 + destinations.size) % destinations.size
    }

    BackHandler { goNext() }

    if (currentScreenIndex in destinations.indices) {
        val current = destinations[currentScreenIndex]
        LiquidExampleChrome(
            title = current.title,
            onPrev = goPrev,
            onNext = goNext
        ) {
            current.content()
        }
    }
}
