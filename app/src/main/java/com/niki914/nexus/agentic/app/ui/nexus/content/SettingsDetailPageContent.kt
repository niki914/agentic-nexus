package com.niki914.nexus.agentic.app.ui.nexus.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.niki914.nexus.agentic.app.R
import com.niki914.nexus.agentic.app.ui.infra.nav.pageViewModel
import com.niki914.nexus.agentic.app.ui.nexus.nav.CustomToolDetailPage
import com.niki914.nexus.agentic.app.ui.nexus.model.SettingsViewModel
import com.niki914.nexus.agentic.app.ui.nexus.nav.McpServerDetailPage
import com.niki914.nexus.agentic.app.ui.nexus.nav.NexusPage
import com.niki914.nexus.agentic.app.ui.nexus.nav.NexusSettingsGroup
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@Composable
fun SettingsDetailPageContent(
    group: NexusSettingsGroup,
    topPadding: Dp,
    hazeState: HazeState,
    onPush: (NexusPage) -> Unit,
) {
    val viewModel = pageViewModel<SettingsViewModel>()
    val uiState by viewModel.uiStateFlow.collectAsState()
    val visibleGroups = uiState.sections.flatMap { it.groups }.toSet()
    if (group !in visibleGroups) {
        return
    }

    if (group == NexusSettingsGroup.BuiltinTools) {
        BuiltinToolsSettingsContent(
            topPadding = topPadding,
            hazeState = hazeState,
        )
        return
    }

    if (group == NexusSettingsGroup.CustomTools) {
        CustomToolsSettingsContent(
            topPadding = topPadding,
            hazeState = hazeState,
            onOpenToolDetail = { name, index ->
                onPush(CustomToolDetailPage(name, index))
            },
        )
        return
    }

    if (group == NexusSettingsGroup.Mcp) {
        McpSettingsContent(
            topPadding = topPadding,
            hazeState = hazeState,
            onOpenServerDetail = { name, index ->
                onPush(McpServerDetailPage(name, index))
            },
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .hazeSource(hazeState)
            .padding(top = topPadding)
            .padding(horizontal = 24.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(group.titleRes),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(group.summaryRes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.ui_settings_detail_placeholder),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
