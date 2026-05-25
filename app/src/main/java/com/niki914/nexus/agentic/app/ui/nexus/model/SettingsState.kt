package com.niki914.nexus.agentic.app.ui.nexus.model

import androidx.annotation.StringRes
import com.niki914.nexus.agentic.app.R
import com.niki914.nexus.agentic.app.ui.nexus.nav.NexusSettingsGroup
import com.niki914.nexus.cb.ComposeMVIViewModel

data class SettingsSectionUiState(
    @StringRes val titleRes: Int,
    val groups: List<NexusSettingsGroup>,
)

data class SettingsUiState(
    val sections: List<SettingsSectionUiState> = emptyList(),
)

sealed interface SettingsIntent

sealed interface SettingsEffect

class SettingsViewModel : ComposeMVIViewModel<SettingsIntent, SettingsUiState, SettingsEffect>() {

    override fun initUiState(): SettingsUiState {
        return buildSettingsUiState(defaultVisibleSettingsGroups())
    }

    override suspend fun handleIntent(intent: SettingsIntent) = Unit

    fun isGroupVisible(group: NexusSettingsGroup): Boolean {
        return currentState.sections.any { section -> group in section.groups }
    }
}

private data class SettingsSectionDefinition(
    @StringRes val titleRes: Int,
    val groups: List<NexusSettingsGroup>,
)

internal fun buildSettingsUiState(
    visibleGroups: List<NexusSettingsGroup>,
): SettingsUiState {
    val visibleGroupSet = visibleGroups.toSet()
    return SettingsUiState(
        sections = settingsSections()
            .mapNotNull { section ->
                val groups = section.groups.filter(visibleGroupSet::contains)
                groups.takeIf { it.isNotEmpty() }?.let {
                    SettingsSectionUiState(
                        titleRes = section.titleRes,
                        groups = groups,
                    )
                }
            }
    )
}

private fun settingsSections(): List<SettingsSectionDefinition> {
    return listOf(
        SettingsSectionDefinition(
            titleRes = R.string.nexus_settings_section_model,
            groups = listOf(
                NexusSettingsGroup.ProviderModel,
                NexusSettingsGroup.Network,
                NexusSettingsGroup.Memory,
            ),
        ),
        SettingsSectionDefinition(
            titleRes = R.string.nexus_settings_section_execution,
            groups = listOf(
                NexusSettingsGroup.BuiltinTools,
                NexusSettingsGroup.ShellRules,
            ),
        ),
        SettingsSectionDefinition(
            titleRes = R.string.nexus_settings_section_integration,
            groups = listOf(
                NexusSettingsGroup.Mcp,
                NexusSettingsGroup.CustomTools,
                NexusSettingsGroup.About,
            ),
        ),
    )
}

private fun defaultVisibleSettingsGroups(): List<NexusSettingsGroup> {
    return listOf(
        NexusSettingsGroup.BuiltinTools,
        NexusSettingsGroup.Mcp,
        NexusSettingsGroup.CustomTools,
    )
}
