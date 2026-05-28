package com.niki914.nexus.agentic.app.ui.nexus.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.niki914.nexus.agentic.app.ui.infra.component.SettingNavigationItem
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsGroupCard
import com.niki914.nexus.agentic.app.ui.infra.nav.pageViewModel
import com.niki914.nexus.agentic.app.ui.nexus.model.SettingsViewModel
import com.niki914.nexus.agentic.app.ui.nexus.nav.NexusSettingsGroup
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@Composable
fun SettingsHomePageContent(
    topPadding: Dp,
    hazeState: HazeState,
    onOpenGroup: (NexusSettingsGroup) -> Unit,
) {
    val viewModel = pageViewModel<SettingsViewModel>()
    val uiState by viewModel.uiStateFlow.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .hazeSource(hazeState)
            .verticalScroll(rememberScrollState())
            .padding(top = topPadding)
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        uiState.sections.forEach { section ->
            SettingsSection(
                title = stringResource(section.titleRes),
                groups = section.groups,
                onOpenGroup = onOpenGroup,
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    groups: List<NexusSettingsGroup>,
    onOpenGroup: (NexusSettingsGroup) -> Unit,
) {
    SettingsGroupCard(title = title) {
        groups.forEachIndexed { index, group ->
            SettingNavigationItem(
                title = stringResource(group.titleRes),
                summary = stringResource(group.summaryRes),
                onClick = { onOpenGroup(group) },
            )
            if (index != groups.lastIndex) {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
        }
    }
}
