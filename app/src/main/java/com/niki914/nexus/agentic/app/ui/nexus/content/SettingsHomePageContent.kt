package com.niki914.nexus.agentic.app.ui.nexus.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.niki914.nexus.agentic.app.ui.infra.ProvideLiquidScreenContentForPreview
import com.niki914.nexus.agentic.app.ui.infra.liquidScreenHazeSource
import com.niki914.nexus.agentic.app.ui.infra.liquidScreenTopPadding
import com.niki914.nexus.agentic.app.ui.infra.component.SettingNavigationItem
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsGroupCard
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsItemDivider
import com.niki914.nexus.agentic.app.ui.infra.nav.pageViewModel
import com.niki914.nexus.agentic.app.ui.nexus.model.SettingsUiState
import com.niki914.nexus.agentic.app.ui.nexus.model.SettingsViewModel
import com.niki914.nexus.agentic.app.ui.nexus.model.buildSettingsUiState
import com.niki914.nexus.agentic.app.ui.nexus.nav.NexusSettingsGroup
import com.niki914.nexus.cb.BaseTheme

@Composable
fun SettingsHomePageContent(
    onOpenGroup: (NexusSettingsGroup) -> Unit,
) {
    val viewModel = pageViewModel<SettingsViewModel>()
    val uiState by viewModel.uiStateFlow.collectAsState()

    SettingsHomePageContentBody(
        uiState = uiState,
        onOpenGroup = onOpenGroup,
    )
}

@Composable
private fun SettingsHomePageContentBody(
    uiState: SettingsUiState,
    onOpenGroup: (NexusSettingsGroup) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .liquidScreenHazeSource()
            .verticalScroll(rememberScrollState())
            .padding(top = liquidScreenTopPadding())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        uiState.sections.forEach { section ->
            SettingsHomeSectionCard(
                title = stringResource(section.titleRes),
                groups = section.groups,
                onOpenGroup = onOpenGroup,
            )
        }
    }
}

@Composable
private fun SettingsHomeSectionCard(
    title: String,
    groups: List<NexusSettingsGroup>,
    onOpenGroup: (NexusSettingsGroup) -> Unit,
) {
    SettingsGroupCard(title = title) {
        groups.forEachIndexed { index, group ->
            val summary = stringResource(group.summaryRes)
            SettingNavigationItem(
                title = stringResource(group.titleRes),
                summary = summary.takeIf { it.isNotBlank() },
                onClick = { onOpenGroup(group) },
            )
            if (index != groups.lastIndex) {
                SettingsItemDivider()
            }
        }
    }
}

@Preview(name = "Settings Home", showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun SettingsHomePageContentPreview() {
    BaseTheme(darkTheme = false, dynamicColor = false) {
        Surface {
            ProvideLiquidScreenContentForPreview(topPadding = 0.dp) {
                SettingsHomePageContentBody(
                    uiState = buildSettingsUiState(hiddenGroups = emptySet()),
                    onOpenGroup = {},
                )
            }
        }
    }
}
