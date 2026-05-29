package com.niki914.nexus.agentic.app.ui.nexus.model

import com.niki914.nexus.agentic.app.ui.nexus.nav.NexusSettingsGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsViewModelTest {

    @Test
    fun buildSettingsUiState_keepsOnlyVisibleGroupsAndDropsEmptySections() {
        val state = buildSettingsUiState(
            listOf(
                NexusSettingsGroup.BuiltinTools,
                NexusSettingsGroup.CustomTools,
            )
        )

        assertEquals(2, state.sections.size)
        assertEquals(
            listOf(NexusSettingsGroup.BuiltinTools),
            state.sections[0].groups,
        )
        assertEquals(
            listOf(NexusSettingsGroup.CustomTools),
            state.sections[1].groups,
        )
    }

    @Test
    fun settingsViewModel_usesDefaultVisibleGroups() {
        val viewModel = SettingsViewModel()
        val state = viewModel.uiStateFlow.value

        assertTrue(state.isGroupVisible(NexusSettingsGroup.ProviderModel))
        assertTrue(state.isGroupVisible(NexusSettingsGroup.BuiltinTools))
        assertFalse(state.isGroupVisible(NexusSettingsGroup.Network))
    }
}

private fun SettingsUiState.isGroupVisible(group: NexusSettingsGroup): Boolean {
    return sections.any { section -> group in section.groups }
}
