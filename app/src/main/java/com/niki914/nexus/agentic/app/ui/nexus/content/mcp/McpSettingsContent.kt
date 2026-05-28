package com.niki914.nexus.agentic.app.ui.nexus.content.mcp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.niki914.nexus.agentic.app.R
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsGroupCard
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsListPageContent
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsToggleListItemCard
import com.niki914.nexus.agentic.app.ui.infra.nav.pageViewModel
import com.niki914.nexus.agentic.app.ui.nexus.model.McpSettingsIntent
import com.niki914.nexus.agentic.app.ui.nexus.model.McpSettingsViewModel
import dev.chrisbanes.haze.HazeState

@Composable
fun McpSettingsContent(
    topPadding: Dp,
    hazeState: HazeState,
    onOpenServerDetail: (serverName: String, serverIndex: Int) -> Unit,
) {
    val viewModel = pageViewModel<McpSettingsViewModel>(factory = McpSettingsViewModelFactory)
    val uiState by viewModel.uiStateFlow.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.sendIntent(McpSettingsIntent.Load)
    }
    val pageDescription = when {
        uiState.isLoading || uiState.items.isNotEmpty() -> stringResource(R.string.mcp_page_description)
        else -> stringResource(R.string.mcp_page_empty_description)
    }

    SettingsListPageContent(
        topPadding = topPadding,
        hazeState = hazeState,
        description = pageDescription,
    ) {
        if (uiState.isLoading) {
            SettingsGroupCard {
                McpListMessage(text = stringResource(R.string.mcp_loading))
            }
        } else if (uiState.items.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                uiState.items.forEachIndexed { index, item ->
                    SettingsToggleListItemCard(
                        title = item.name,
                        checked = item.enabled,
                        enabled = !uiState.isSaving,
                        onCheckedChange = { checked ->
                            viewModel.sendIntent(McpSettingsIntent.ItemEnabledChanged(index, checked))
                        },
                        onClick = {
                            onOpenServerDetail(item.name, index)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun McpListMessage(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
    )
}
