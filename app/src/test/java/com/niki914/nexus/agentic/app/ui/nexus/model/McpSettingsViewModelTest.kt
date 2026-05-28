package com.niki914.nexus.agentic.app.ui.nexus.model

import android.content.Context
import android.content.ContextWrapper
import com.niki914.nexus.agentic.app.R
import com.niki914.nexus.agentic.mod.LocalSettings
import com.niki914.nexus.agentic.repo.LocalSettingsStore
import com.niki914.nexus.agentic.repo.XRepo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeMcpServer as McpServer

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

    @Test
    fun save_withBlankName_setsNameErrorAndSkipsPersistence() = runTest {
        installStore(LocalSettings())
        val viewModel = McpSettingsViewModel()

        viewModel.sendIntent(McpSettingsIntent.Load)
        advanceUntilIdle()
        viewModel.sendIntent(McpSettingsIntent.StartCreate)
        viewModel.sendIntent(McpSettingsIntent.UrlChanged("http://127.0.0.1:51338/mcp"))
        viewModel.sendIntent(McpSettingsIntent.Save)
        advanceUntilIdle()

        val state = viewModel.uiStateFlow.value
        assertEquals(R.string.mcp_error_name_required, state.formState.nameErrorResId)
        assertTrue(XRepo.mcp.list().isEmpty())
        assertFalse(state.isSaving)
    }

    @Test
    fun save_withBlankName_emitsFocusNameEveryTime() = runTest {
        installStore(LocalSettings())
        val viewModel = McpSettingsViewModel()
        val effects = mutableListOf<McpSettingsEffect>()
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEffect.take(2).toList(effects)
        }

        viewModel.sendIntent(McpSettingsIntent.Load)
        advanceUntilIdle()
        viewModel.sendIntent(McpSettingsIntent.StartCreate)
        viewModel.sendIntent(McpSettingsIntent.UrlChanged("http://127.0.0.1:51338/mcp"))
        viewModel.sendIntent(McpSettingsIntent.Save)
        viewModel.sendIntent(McpSettingsIntent.Save)
        advanceUntilIdle()

        assertEquals(
            listOf(McpSettingsEffect.FocusName, McpSettingsEffect.FocusName),
            effects,
        )
        collectJob.cancel()
    }

    @Test
    fun save_withInvalidHeadersJson_setsHeadersErrorAndSkipsPersistence() = runTest {
        installStore(LocalSettings())
        val viewModel = McpSettingsViewModel()

        viewModel.sendIntent(McpSettingsIntent.Load)
        advanceUntilIdle()
        viewModel.sendIntent(McpSettingsIntent.StartCreate)
        viewModel.sendIntent(McpSettingsIntent.NameChanged("demo"))
        viewModel.sendIntent(McpSettingsIntent.UrlChanged("http://127.0.0.1:51338/mcp"))
        viewModel.sendIntent(McpSettingsIntent.HeadersChanged("{"))
        viewModel.sendIntent(McpSettingsIntent.Save)
        advanceUntilIdle()

        val state = viewModel.uiStateFlow.value
        assertEquals(R.string.mcp_error_headers_invalid_json, state.formState.headersErrorResId)
        assertTrue(XRepo.mcp.list().isEmpty())
        assertFalse(state.isSaving)
    }

    @Test
    fun save_withNonHttpScheme_setsUrlErrorAndSkipsPersistence() = runTest {
        installStore(LocalSettings())
        val viewModel = McpSettingsViewModel()

        viewModel.sendIntent(McpSettingsIntent.Load)
        advanceUntilIdle()
        viewModel.sendIntent(McpSettingsIntent.StartCreate)
        viewModel.sendIntent(McpSettingsIntent.NameChanged("demo"))
        viewModel.sendIntent(McpSettingsIntent.UrlChanged("ftp://127.0.0.1:51338/mcp"))
        viewModel.sendIntent(McpSettingsIntent.Save)
        advanceUntilIdle()

        val state = viewModel.uiStateFlow.value
        assertEquals(R.string.mcp_error_url_invalid, state.formState.urlErrorResId)
        assertTrue(XRepo.mcp.list().isEmpty())
        assertFalse(state.isSaving)
    }

    @Test
    fun save_withHeaders_persistsHeadersAndEmitsExitDetail() = runTest {
        installStore(LocalSettings())
        val viewModel = McpSettingsViewModel()
        val effects = mutableListOf<McpSettingsEffect>()
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEffect.take(1).toList(effects)
        }

        viewModel.sendIntent(McpSettingsIntent.Load)
        advanceUntilIdle()
        viewModel.sendIntent(McpSettingsIntent.StartCreate)
        viewModel.sendIntent(McpSettingsIntent.NameChanged("demo"))
        viewModel.sendIntent(McpSettingsIntent.UrlChanged("http://127.0.0.1:51338/mcp"))
        viewModel.sendIntent(
            McpSettingsIntent.HeadersChanged("""{"Authorization":"Bearer xxx"}""")
        )
        viewModel.sendIntent(McpSettingsIntent.Save)
        advanceUntilIdle()

        val state = viewModel.uiStateFlow.value
        assertNull(state.formState.nameErrorResId)
        assertNull(state.formState.urlErrorResId)
        assertNull(state.formState.headersErrorResId)
        assertEquals(
            listOf(
                McpServer(
                    name = "demo",
                    url = "http://127.0.0.1:51338/mcp",
                    enabled = true,
                    headers = mapOf("Authorization" to "Bearer xxx"),
                )
            ),
            XRepo.mcp.list(),
        )
        assertEquals(listOf(McpSettingsEffect.ExitDetail), effects)
        collectJob.cancel()
    }

    @Test
    fun deleteCurrent_deletesServerAndEmitsExitDetail() = runTest {
        installStore(
            buildLocalSettings(
                listOf(
                    McpServer(
                        name = "demo",
                        url = "http://127.0.0.1:51338/mcp",
                        enabled = true,
                    )
                )
            )
        )
        val viewModel = McpSettingsViewModel()
        val effects = mutableListOf<McpSettingsEffect>()
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEffect.take(1).toList(effects)
        }

        viewModel.sendIntent(McpSettingsIntent.Load)
        advanceUntilIdle()
        viewModel.sendIntent(McpSettingsIntent.StartEdit(0))
        advanceUntilIdle()
        viewModel.sendIntent(McpSettingsIntent.DeleteCurrent)
        advanceUntilIdle()

        assertTrue(XRepo.mcp.list().isEmpty())
        assertEquals(listOf(McpSettingsEffect.ExitDetail), effects)
        collectJob.cancel()
    }

    @Test
    fun deleteCurrent_refreshesOtherLoadedMcpSettingsViewModels() = runTest {
        installStore(
            buildLocalSettings(
                listOf(
                    McpServer(
                        name = "demo",
                        url = "http://127.0.0.1:51338/mcp",
                        enabled = true,
                    )
                )
            )
        )
        val listViewModel = McpSettingsViewModel()
        val detailViewModel = McpSettingsViewModel()

        listViewModel.sendIntent(McpSettingsIntent.Load)
        detailViewModel.sendIntent(McpSettingsIntent.Load)
        advanceUntilIdle()
        assertEquals(1, listViewModel.uiStateFlow.value.items.size)

        detailViewModel.sendIntent(McpSettingsIntent.StartEdit(0))
        detailViewModel.sendIntent(McpSettingsIntent.DeleteCurrent)
        advanceUntilIdle()

        assertTrue(XRepo.mcp.list().isEmpty())
        assertTrue(listViewModel.uiStateFlow.value.items.isEmpty())
    }

    @Test
    fun startEdit_formatsHeadersInputAsSortedMultilineJson() = runTest {
        installStore(
            localSettings(
                """
                {
                  "mcp_servers": [
                    {
                      "name": "demo",
                      "url": "http://127.0.0.1:51338/mcp",
                      "enabled": true,
                      "headers": {
                        "X-Trace-Id": "trace-1",
                        "Authorization": "Bearer xxx"
                      }
                    }
                  ]
                }
                """.trimIndent()
            )
        )
        val viewModel = McpSettingsViewModel()

        viewModel.sendIntent(McpSettingsIntent.Load)
        advanceUntilIdle()
        viewModel.sendIntent(McpSettingsIntent.StartEdit(0))
        advanceUntilIdle()

        assertEquals(
            """
            {
              "Authorization": "Bearer xxx",
              "X-Trace-Id": "trace-1"
            }
            """.trimIndent(),
            viewModel.uiStateFlow.value.formState.headersInput,
        )
    }

    @Test
    fun save_renameFailureDoesNotDeleteExistingServer() = runTest {
        val existing = McpServer(name = "old", url = "http://127.0.0.1:1/mcp", enabled = true)
        installStore(buildLocalSettings(listOf(existing)), failWrites = true)
        val viewModel = McpSettingsViewModel()

        viewModel.sendIntent(McpSettingsIntent.Load)
        advanceUntilIdle()
        viewModel.sendIntent(McpSettingsIntent.StartEdit(0))
        viewModel.sendIntent(McpSettingsIntent.NameChanged("new"))
        viewModel.sendIntent(McpSettingsIntent.UrlChanged("http://127.0.0.1:2/mcp"))
        viewModel.sendIntent(McpSettingsIntent.Save)
        advanceUntilIdle()

        assertEquals(listOf(existing), XRepo.mcp.list())
        assertFalse(viewModel.uiStateFlow.value.isSaving)
    }

    private fun installStore(
        initialSettings: LocalSettings,
        failWrites: Boolean = false,
    ) {
        XRepo.installStoreForTest(FakeLocalSettingsStore(initialSettings, failWrites))
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
        private val failWrites: Boolean,
    ) : LocalSettingsStore {
        private var settings: LocalSettings = initialSettings

        override suspend fun read(context: Context): LocalSettings = settings

        override suspend fun write(context: Context, settings: LocalSettings) {
            if (failWrites) {
                error("write failed")
            }
            this.settings = settings
        }
    }
}
