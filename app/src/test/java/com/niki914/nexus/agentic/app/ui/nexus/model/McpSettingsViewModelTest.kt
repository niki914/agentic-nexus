package com.niki914.nexus.agentic.app.ui.nexus.model

import android.content.Context
import android.content.ContextWrapper
import com.niki914.nexus.agentic.mod.LocalSettings
import com.niki914.nexus.agentic.repo.LocalSettingsStore
import com.niki914.nexus.agentic.repo.XRepo
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeMcpServer as McpServer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
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
        assertEquals("请输入名称", state.formState.nameError)
        assertTrue(XRepo.mcp.list().isEmpty())
        assertFalse(state.isSaving)
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
        assertEquals("请求头不是合法 JSON", state.formState.headersError)
        assertTrue(XRepo.mcp.list().isEmpty())
        assertFalse(state.isSaving)
    }

    @Test
    fun save_withHeaders_persistsHeadersAndEmitsExitDetail() = runTest {
        installStore(LocalSettings())
        val viewModel = McpSettingsViewModel()
        val effectDeferred = async {
            withTimeout(1_000) { viewModel.uiEffect.first() }
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
        assertNull(state.formState.nameError)
        assertNull(state.formState.urlError)
        assertNull(state.formState.headersError)
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
        assertEquals(McpSettingsEffect.ExitDetail, effectDeferred.await())
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
        val effectDeferred = async {
            withTimeout(1_000) { viewModel.uiEffect.first() }
        }

        viewModel.sendIntent(McpSettingsIntent.Load)
        advanceUntilIdle()
        viewModel.sendIntent(McpSettingsIntent.StartEdit(0))
        advanceUntilIdle()
        viewModel.sendIntent(McpSettingsIntent.DeleteCurrent)
        advanceUntilIdle()

        assertTrue(XRepo.mcp.list().isEmpty())
        assertEquals(McpSettingsEffect.ExitDetail, effectDeferred.await())
    }

    @Test
    fun startEdit_formatsHeadersInputAsSortedMultilineJson() = runTest {
        val viewModel = McpSettingsViewModel(
            listServers = {
                listOf(
                    McpServer(
                        name = "demo",
                        url = "http://127.0.0.1:51338/mcp",
                        enabled = true,
                        headers = linkedMapOf(
                            "X-Trace-Id" to "trace-1",
                            "Authorization" to "Bearer xxx",
                        ),
                    )
                )
            }
        )

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
        installStore(buildLocalSettings(listOf(existing)))
        var deleteCalled = false
        val viewModel = McpSettingsViewModel(
            listServers = { XRepo.mcp.list() },
            saveServer = { fail("saveServer should not be called for rename") },
            replaceServer = { _, _ -> error("replace failed") },
            deleteServer = {
                deleteCalled = true
                XRepo.mcp.delete(it)
            },
            setServerEnabled = { _, _ -> fail("setServerEnabled should not be called") },
        )

        viewModel.sendIntent(McpSettingsIntent.Load)
        advanceUntilIdle()
        viewModel.sendIntent(McpSettingsIntent.StartEdit(0))
        viewModel.sendIntent(McpSettingsIntent.NameChanged("new"))
        viewModel.sendIntent(McpSettingsIntent.UrlChanged("http://127.0.0.1:2/mcp"))
        viewModel.sendIntent(McpSettingsIntent.Save)
        advanceUntilIdle()

        assertFalse(deleteCalled)
        assertEquals(listOf(existing), XRepo.mcp.list())
        assertFalse(viewModel.uiStateFlow.value.isSaving)
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
