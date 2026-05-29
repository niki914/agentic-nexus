package com.niki914.nexus.agentic.repo

import android.content.Context
import android.content.ContextWrapper
import com.niki914.nexus.agentic.mod.LocalSettings
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeCustomTool as CustomTool
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeLlmConfig as LlmConfig
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeMcpServer as McpServer
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeMcpTool as McpTool

class XRepoTest {
    private val context: Context = object : ContextWrapper(null) {
        override fun getApplicationContext(): Context = this
    }

    @After
    fun tearDown() {
        XRepo.resetForTest()
    }

    @Test
    fun tryPutDefaultSettings_writesPromptAndWechatToolWhenOnboardingIsNotCompleted() = runTest {
        val store = FakeLocalSettingsStore(LocalSettings())
        XRepo.installStoreForTest(store)
        XRepo.init(context)

        val updated = XRepo.tryPutDefaultSettings()

        assertTrue(updated)
        assertEquals(1, store.writeCount)
        assertEquals(
            LocalSettingsDefaults.DEFAULT_SYSTEM_PROMPT.trimIndent(),
            store.settings.prompt
        )
        assertEquals("", store.settings.endpoint)
        assertEquals("", store.settings.apiKey)
        assertEquals("", store.settings.model)
        assertNull(store.settings.mcpServers)
        assertEquals(
            listOf(
                CustomTool(
                    name = "launch_wechat",
                    description = "启动微信",
                    command = "am start -n com.tencent.mm/com.tencent.mm.ui.LauncherUI",
                )
            ),
            LocalSettingsCodec.parseCustomTools(store.settings),
        )
    }

    @Test
    fun tryPutDefaultSettings_skipsWhenOnboardingIsCompleted() = runTest {
        val store = FakeLocalSettingsStore(
            LocalSettingsCodec.withBoolean(LocalSettings(), "onboarding_completed", true)
        )
        XRepo.installStoreForTest(store)
        XRepo.init(context)

        val updated = XRepo.tryPutDefaultSettings()

        assertFalse(updated)
        assertEquals(0, store.writeCount)
        assertEquals("", store.settings.prompt)
        assertNull(store.settings.customTools)
    }

    @Test
    fun saveLlmAccess_updatesOnlyAccessFields() = runTest {
        val store = FakeLocalSettingsStore(
            localSettings(
                """
                {
                  "provider":"old",
                  "endpoint":"https://old.example",
                  "api_key":"old-key",
                  "model":"old-model",
                  "prompt":"base",
                  "proxy":"http://proxy",
                  "memory_prompt":"memory"
                }
                """.trimIndent()
            )
        )
        XRepo.installStoreForTest(store)
        XRepo.init(context)

        XRepo.saveLlmAccess(
            provider = "openai",
            endpoint = "https://api.example",
            model = "gpt-test",
            apiKey = "secret",
        )

        assertEquals(1, store.writeCount)
        assertEquals(
            LlmConfig(
                provider = "openai",
                endpoint = "https://api.example",
                apiKey = "secret",
                model = "gpt-test",
                prompt = "base",
                proxy = "http://proxy",
                memoryPrompt = "memory",
            ),
            LocalSettingsCodec.parseLlm(store.settings),
        )
    }

    @Test
    fun mcpSave_replacesByNameAndPreservesOtherServers() = runTest {
        val store = FakeLocalSettingsStore(
            LocalSettingsCodec.withMcpServers(
                LocalSettings(),
                listOf(
                    McpServer("aslocate", "http://old.example/mcp"),
                    McpServer("weather", "http://weather.example/mcp"),
                ),
            )
        )
        XRepo.installStoreForTest(store)
        XRepo.init(context)

        XRepo.mcp.save(McpServer("aslocate", "http://new.example/mcp", enabled = false))

        assertEquals(
            listOf(
                McpServer("aslocate", "http://new.example/mcp", enabled = false),
                McpServer("weather", "http://weather.example/mcp"),
            ),
            XRepo.mcp.list(),
        )
    }

    @Test
    fun mcpClearCache_removesOnlyTargetCacheKey() = runTest {
        val first = McpServer("aslocate", "http://127.0.0.1:51338/mcp")
        val second = McpServer("weather", "http://127.0.0.1:51339/mcp")
        val store = FakeLocalSettingsStore(
            LocalSettingsCodec.withMcpCache(
                LocalSettingsCodec.withMcpCache(
                    LocalSettings(),
                    first.url,
                    first.headers,
                    listOf(McpTool("lookupSymbol", "Lookup", """{"type":"object"}""")),
                ),
                second.url,
                second.headers,
                listOf(McpTool("getWeather", "Weather", """{"type":"object"}""")),
            )
        )
        XRepo.installStoreForTest(store)
        XRepo.init(context)

        XRepo.mcp.clearCache(first)

        assertEquals(emptyList<McpTool>(), XRepo.mcp.cachedTools(first))
        assertEquals(
            listOf(McpTool("getWeather", "Weather", """{"type":"object"}""")),
            XRepo.mcp.cachedTools(second)
        )
    }

    @Test
    fun customToolSave_rejectsUnsafeCommand() = runTest {
        val store = FakeLocalSettingsStore(LocalSettings())
        XRepo.installStoreForTest(store)
        XRepo.init(context)

        val validation = XRepo.customTools.save(
            CustomTool(
                name = "wipe_data",
                description = "Dangerous",
                command = "rm -rf /data/local/tmp/cache",
            )
        )

        assertNotNull(validation)
        assertEquals("command", validation!!.field)
        assertEquals(0, store.writeCount)
    }

    @Test
    fun customToolReplace_renamesAndPreservesOtherTools() = runTest {
        val store = FakeLocalSettingsStore(
            LocalSettingsCodec.withCustomTools(
                LocalSettings(),
                listOf(
                    CustomTool("old_name", "Old description", "dumpsys battery"),
                    CustomTool("other_tool", "Other description", "settings list system"),
                ),
            )
        )
        XRepo.installStoreForTest(store)
        XRepo.init(context)

        val validation = XRepo.customTools.replace(
            previousName = "old_name",
            tool = CustomTool("new_name", "New description", "pm list packages", enabled = false),
        )

        assertNull(validation)
        assertEquals(1, store.writeCount)
        assertEquals(
            listOf(
                CustomTool("other_tool", "Other description", "settings list system"),
                CustomTool("new_name", "New description", "pm list packages", enabled = false),
            ),
            XRepo.customTools.list(),
        )
    }

    @Test
    fun customToolReplace_rejectsDuplicateName() = runTest {
        val initialTools = listOf(
            CustomTool("old_name", "Old description", "dumpsys battery"),
            CustomTool("existing_tool", "Existing description", "settings list system"),
        )
        val store = FakeLocalSettingsStore(
            LocalSettingsCodec.withCustomTools(LocalSettings(), initialTools)
        )
        XRepo.installStoreForTest(store)
        XRepo.init(context)

        val validation = XRepo.customTools.replace(
            previousName = "old_name",
            tool = CustomTool("existing_tool", "New description", "pm list packages"),
        )

        assertNotNull(validation)
        assertEquals("name", validation!!.field)
        assertEquals("Already exists in custom_tools.", validation.message)
        assertEquals(0, store.writeCount)
        assertEquals(initialTools, XRepo.customTools.list())
    }

    @Test
    fun customToolReplace_rejectsUnsafeCommand() = runTest {
        val initialTools = listOf(
            CustomTool("old_name", "Old description", "dumpsys battery"),
        )
        val store = FakeLocalSettingsStore(
            LocalSettingsCodec.withCustomTools(LocalSettings(), initialTools)
        )
        XRepo.installStoreForTest(store)
        XRepo.init(context)

        val validation = XRepo.customTools.replace(
            previousName = "old_name",
            tool = CustomTool("new_name", "New description", "rm -rf /data/local/tmp/cache"),
        )

        assertNotNull(validation)
        assertEquals("command", validation!!.field)
        assertEquals(0, store.writeCount)
        assertEquals(initialTools, XRepo.customTools.list())
    }

    @Test
    fun builtinSetEnabled_rejectsUnknownTool() = runTest {
        val store = FakeLocalSettingsStore(LocalSettings())
        XRepo.installStoreForTest(store)
        XRepo.init(context)

        val validation = XRepo.builtinTools.setEnabled("unknown_tool", true)

        assertNotNull(validation)
        assertEquals("name", validation!!.field)
        assertEquals(0, store.writeCount)
    }

    @Test
    fun customToolSave_acceptsSafeCommand() = runTest {
        val store = FakeLocalSettingsStore(LocalSettings())
        XRepo.installStoreForTest(store)
        XRepo.init(context)

        val validation = XRepo.customTools.save(
            CustomTool(
                name = "battery_status",
                description = "Battery status",
                command = "dumpsys battery",
            )
        )

        assertNull(validation)
        assertEquals(1, store.writeCount)
        assertEquals(
            listOf(CustomTool("battery_status", "Battery status", "dumpsys battery")),
            XRepo.customTools.list(),
        )
    }

    private fun localSettings(json: String): LocalSettings {
        return LocalSettings(Json.parseToJsonElement(json).jsonObject)
    }

    private class FakeLocalSettingsStore(
        initialSettings: LocalSettings,
    ) : LocalSettingsStore {
        var settings: LocalSettings = initialSettings
            private set
        var writeCount: Int = 0
            private set

        override suspend fun read(context: Context): LocalSettings = settings

        override suspend fun write(context: Context, settings: LocalSettings) {
            this.settings = settings
            writeCount++
        }
    }
}
