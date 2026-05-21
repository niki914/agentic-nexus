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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.niki914.nexus.agentic.app.R
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsGroupCard
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsNavigationRow
import com.niki914.nexus.agentic.app.ui.nexus.nav.NexusSettingsGroup
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@Composable
fun SettingsHomePageContent(
    topPadding: Dp,
    hazeState: HazeState,
    onOpenGroup: (NexusSettingsGroup) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .hazeSource(hazeState)
            .verticalScroll(rememberScrollState())
            .padding(top = topPadding)
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        SettingsSection(
            title = stringResource(R.string.nexus_settings_section_model),
            groups = listOf(
                NexusSettingsGroup.ProviderModel,
                NexusSettingsGroup.Network,
                NexusSettingsGroup.Memory,
            ),
            onOpenGroup = onOpenGroup,
        )
        SettingsSection(
            title = stringResource(R.string.nexus_settings_section_execution),
            groups = listOf(
                NexusSettingsGroup.BuiltinTools,
                NexusSettingsGroup.ShellRules,
            ),
            onOpenGroup = onOpenGroup,
        )
        SettingsSection(
            title = stringResource(R.string.nexus_settings_section_integration),
            groups = listOf(
                NexusSettingsGroup.Mcp,
                NexusSettingsGroup.CustomTools,
                NexusSettingsGroup.About,
            ),
            onOpenGroup = onOpenGroup,
        )
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
            SettingsNavigationRow(
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
