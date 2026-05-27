package com.niki914.nexus.agentic.app.ui.nexus.model

import com.niki914.nexus.agentic.mod.LocalSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class McpSettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun load_readsSavedServersIntoUiState() = runTest {
        val viewModel = McpSettingsViewModel(
            loadSettings = {
                buildLocalSettings(
                    listOf(
                        McpServerItem(name = "alpha", url = "http://127.0.0.1:3000/mcp", enabled = true),
                        McpServerItem(name = "beta", url = "http://127.0.0.1:4000/mcp", enabled = false),
                    )
                )
            },
            saveSettings = { _ -> },
        )

        viewModel.sendIntent(McpSettingsIntent.Load)
        advanceUntilIdle()

        val state = viewModel.uiStateFlow.value
        assertFalse(state.isLoading)
        assertEquals(2, state.items.size)
        assertEquals("alpha", state.items[0].name)
        assertEquals("http://127.0.0.1:4000/mcp", state.items[1].url)
        assertFalse(state.items[1].enabled)
    }

    @Test
    fun save_persistsTrimmedServerAndUpdatesUiState() = runTest {
        var savedSettings: LocalSettings? = null
        val viewModel = McpSettingsViewModel(
            loadSettings = { buildLocalSettings(emptyList()) },
            saveSettings = { settings -> savedSettings = settings },
        )

        viewModel.sendIntent(McpSettingsIntent.Load)
        advanceUntilIdle()
        viewModel.sendIntent(McpSettingsIntent.NameChanged(" demo "))
        viewModel.sendIntent(McpSettingsIntent.UrlChanged(" http://127.0.0.1:51338/mcp "))
        viewModel.sendIntent(McpSettingsIntent.Save)
        advanceUntilIdle()

        val state = viewModel.uiStateFlow.value
        assertEquals(1, state.items.size)
        assertEquals("demo", state.items.single().name)
        assertEquals("http://127.0.0.1:51338/mcp", state.items.single().url)
        assertTrue(savedSettings?.props?.containsKey("mcp_servers") == true)
    }

    @Test
    fun save_rejectsDuplicateName() = runTest {
        val viewModel = McpSettingsViewModel(
            loadSettings = {
                buildLocalSettings(
                    listOf(McpServerItem(name = "demo", url = "http://127.0.0.1:1/mcp", enabled = true))
                )
            },
            saveSettings = { _ -> },
        )

        viewModel.sendIntent(McpSettingsIntent.Load)
        advanceUntilIdle()
        viewModel.sendIntent(McpSettingsIntent.StartCreate)
        viewModel.sendIntent(McpSettingsIntent.NameChanged("demo"))
        viewModel.sendIntent(McpSettingsIntent.UrlChanged("http://127.0.0.1:2/mcp"))
        viewModel.sendIntent(McpSettingsIntent.Save)
        advanceUntilIdle()

        val state = viewModel.uiStateFlow.value
        assertEquals(1, state.items.size)
        assertFalse(state.isSaving)
    }

    private fun buildLocalSettings(items: List<McpServerItem>): LocalSettings {
        return buildUpdatedLocalSettings(LocalSettings(JsonObject(emptyMap())), items)
    }
}
