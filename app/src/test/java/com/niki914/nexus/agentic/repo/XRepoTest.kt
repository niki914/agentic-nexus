package com.niki914.nexus.agentic.repo

import android.content.Context
import android.content.ContextWrapper
import com.niki914.nexus.agentic.mod.LocalSettings
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class XRepoTest {
    private val context: Context = object : ContextWrapper(null) {
        override fun getApplicationContext(): Context = this
    }

    @After
    fun tearDown() {
        XRepo.resetForTest()
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
        assertEquals(listOf(McpTool("getWeather", "Weather", """{"type":"object"}""")), XRepo.mcp.cachedTools(second))
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
