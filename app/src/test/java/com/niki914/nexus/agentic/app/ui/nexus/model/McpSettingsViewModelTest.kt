package com.niki914.nexus.agentic.app.ui.nexus.model

import android.content.Context
import android.content.ContextWrapper
import com.niki914.nexus.agentic.mod.LocalSettings
import com.niki914.nexus.agentic.repo.LocalSettingsStore
import com.niki914.nexus.agentic.repo.McpServer
import com.niki914.nexus.agentic.repo.XRepo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class McpSettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context: Context = object : ContextWrapper(null) {
        override fun getApplicationContext(): Context = this
    }

    @After
    fun tearDown() {
        XRepo.resetForTest()
    }

    @Test
    fun load_readsSavedServersIntoUiState() = runTest {
        installStore(
            buildLocalSettings(
                listOf(
                    McpServer(name = "alpha", url = "http://127.0.0.1:3000/mcp", enabled = true),
                    McpServer(name = "beta", url = "http://127.0.0.1:4000/mcp", enabled = false),
                )
            )
        )
        val viewModel = McpSettingsViewModel()

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
        installStore(LocalSettings())
        val viewModel = McpSettingsViewModel()

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
        assertEquals(
            listOf(McpServer("demo", "http://127.0.0.1:51338/mcp")),
            XRepo.mcp.list(),
        )
    }

    @Test
    fun save_rejectsDuplicateName() = runTest {
        installStore(
            buildLocalSettings(
                listOf(McpServer(name = "demo", url = "http://127.0.0.1:1/mcp", enabled = true))
            )
        )
        val viewModel = McpSettingsViewModel()

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
        assertEquals(1, XRepo.mcp.list().size)
    }

    private fun installStore(initialSettings: LocalSettings) {
        XRepo.installStoreForTest(FakeLocalSettingsStore(initialSettings))
        XRepo.init(context)
    }

    private fun buildLocalSettings(items: List<McpServer>): LocalSettings {
        val serversJson = items.joinToString(separator = ",") { server ->
            """{"name":"${server.name}","url":"${server.url}","enabled":${server.enabled}}"""
        }
        return localSettings("""{"mcp_servers":[$serversJson]}""")
    }

    private fun localSettings(json: String): LocalSettings {
        return LocalSettings(Json.parseToJsonElement(json).jsonObject)
    }

    private class FakeLocalSettingsStore(
        initialSettings: LocalSettings,
    ) : LocalSettingsStore {
        private var settings: LocalSettings = initialSettings

        override suspend fun read(context: Context): LocalSettings = settings

        override suspend fun write(context: Context, settings: LocalSettings) {
            this.settings = settings
        }
    }
}
