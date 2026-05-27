package com.niki914.nexus.agentic.app.ui.nexus.content

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsListPageContent
import dev.chrisbanes.haze.HazeState

@Composable
fun CustomToolDetailContent(
    topPadding: Dp,
    hazeState: HazeState,
) {
    SettingsListPageContent(
        topPadding = topPadding,
        hazeState = hazeState,
    ) {
    }
}
