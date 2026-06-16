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
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeExecutionRule as ExecutionRule
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeExecutionRuleEnabledMode as ExecutionRuleEnabledMode
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
        assertEquals(
            LocalSettingsDefaults.defaultMemories,
            LocalSettingsCodec.parseMemories(store.settings),
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
        val executionRules = LocalSettingsCodec.parseExecutionRules(store.settings)
        assertEquals(3, executionRules.size)
        assertEquals(
            ExecutionRule(
                id = "builtin-dangerous-delete",
                name = "危险删改",
                enabledMode = ExecutionRuleEnabledMode.LOCKED_ONLY,
                patterns = listOf(
                    "\\brm\\s+-rf\\b",
                    "\\brm\\s+-(?=[^\\s]*r)(?=[^\\s]*f)[^\\s]*\\b",
                    "\\brm\\s+-r\\s+-f\\b",
                    "\\brm\\s+(?=[^\\n]*--recursive\\b)(?=[^\\n]*--force\\b)[^\\n]*",
                    "\\brm\\s+(?=[^\\n]*-(?:[^\\s-]*r[^\\s-]*|-[^-\\s]*recursive)\\b)(?=[^\\n]*-(?:[^\\s-]*f[^\\s-]*|-[^-\\s]*force)\\b)[^\\n]*",
                    "\\bmkfs\\b",
                ),
            ),
            executionRules[0],
        )
    }

    @Test
    fun defaultMemoriesDescribeDomainSettingsTree() {
        val text = LocalSettingsDefaults.defaultMemories.joinToString(separator = "\n")

        assertFalse(text.contains("local_settings.json"))
        assertTrue(text.contains("files/settings"))
        assertTrue(text.contains("com.niki914.nexus.agentic"))
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
    fun executionRulesList_fallsBackToDefaultsWhenFieldIsMissing() = runTest {
        val store = FakeLocalSettingsStore(
            LocalSettingsCodec.withBoolean(LocalSettings(), "onboarding_completed", true)
        )
        XRepo.installStoreForTest(store)
        XRepo.init(context)

        val rules = XRepo.executionRules.list()

        assertEquals(LocalSettingsDefaults.defaultExecutionRules, rules)
        assertEquals(0, store.writeCount)
    }

    @Test
    fun executionRulesList_respectsExplicitEmptyList() = runTest {
        val store = FakeLocalSettingsStore(
            LocalSettingsCodec.withExecutionRules(LocalSettings(), emptyList())
        )
        XRepo.installStoreForTest(store)
        XRepo.init(context)

        assertTrue(XRepo.executionRules.list().isEmpty())
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
    fun memoryApi_replacesAndMutatesMemories() = runTest {
        val store = FakeLocalSettingsStore(LocalSettings())
        XRepo.installStoreForTest(store)
        XRepo.init(context)

        XRepo.memory.replaceAll(listOf(" A ", " ", "B"))
        XRepo.memory.add(" C ")
        XRepo.memory.update(1, " B2 ")
        XRepo.memory.delete(0)
        val writeCountBeforeOutOfBoundsUpdate = store.writeCount
        XRepo.memory.update(99, "ignored")
        assertEquals(writeCountBeforeOutOfBoundsUpdate, store.writeCount)
        val writeCountBeforeOutOfBoundsDelete = store.writeCount
        XRepo.memory.delete(-1)
        assertEquals(writeCountBeforeOutOfBoundsDelete, store.writeCount)
        val writeCountBeforeBlankAdd = store.writeCount
        XRepo.memory.add(" ")

        assertEquals(writeCountBeforeBlankAdd, store.writeCount)
        assertEquals(listOf("B2", "C"), XRepo.memory.list())
        assertEquals(listOf("B2", "C"), LocalSettingsCodec.parseMemories(store.settings))
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
    fun executionRulesApi_savesReplacesDeletesAndUpdatesEnabledMode() = runTest {
        val initialRule = ExecutionRule(
            id = "rule-1",
            name = "Rule One",
            enabledMode = ExecutionRuleEnabledMode.ALWAYS,
            patterns = listOf("rm -rf"),
        )
        val store = FakeLocalSettingsStore(
            LocalSettingsCodec.withExecutionRules(LocalSettings(), listOf(initialRule))
        )
        XRepo.installStoreForTest(store)
        XRepo.init(context)

        XRepo.executionRules.save(
            ExecutionRule(
                id = "rule-2",
                name = "Rule Two",
                enabledMode = ExecutionRuleEnabledMode.DISABLED,
                patterns = listOf("mkfs"),
            )
        )
        XRepo.executionRules.replace(
            previousId = "rule-1",
            rule = ExecutionRule(
                id = "rule-3",
                name = "Rule Three",
                enabledMode = ExecutionRuleEnabledMode.ALWAYS,
                patterns = listOf("su"),
            )
        )
        XRepo.executionRules.setEnabledMode("rule-2", ExecutionRuleEnabledMode.LOCKED_ONLY)
        XRepo.executionRules.delete("missing")
        XRepo.executionRules.delete("rule-3")

        assertEquals(
            listOf(
                ExecutionRule(
                    id = "rule-2",
                    name = "Rule Two",
                    enabledMode = ExecutionRuleEnabledMode.LOCKED_ONLY,
                    patterns = listOf("mkfs"),
                )
            ),
            XRepo.executionRules.list(),
        )
        assertEquals(5, store.writeCount)
    }

    @Test
    fun customToolSave_rejectsUnsafeCommand() = runTest {
        val store = FakeLocalSettingsStore(settingsWithUnsafeRule())
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
            LocalSettingsCodec.withCustomTools(settingsWithUnsafeRule(), initialTools)
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
    fun builtinListContainsTerminalAndNotRunCommand() = runTest {
        val store = FakeLocalSettingsStore(LocalSettings())
        XRepo.installStoreForTest(store)
        XRepo.init(context)

        val names = XRepo.builtinTools.list().map { it.name }

        assertTrue(names.contains("terminal"))
        assertFalse(names.contains("run_command"))
    }

    @Test
    fun builtinTerminalInheritsLegacyRunCommandDisabledFlag() = runTest {
        val settings = LocalSettingsCodec.withBuiltinFlag(LocalSettings(), "run_command", false)
        val store = FakeLocalSettingsStore(settings)
        XRepo.installStoreForTest(store)
        XRepo.init(context)

        val terminal = XRepo.builtinTools.list().single { it.name == "terminal" }

        assertFalse(terminal.enabled)
        assertEquals(0, store.writeCount)
    }

    @Test
    fun builtinSetEnabled_acceptsTerminalAndRejectsRunCommand() = runTest {
        val store = FakeLocalSettingsStore(LocalSettings())
        XRepo.installStoreForTest(store)
        XRepo.init(context)

        val terminalValidation = XRepo.builtinTools.setEnabled("terminal", false)
        val runCommandValidation = XRepo.builtinTools.setEnabled("run_command", false)
        val flags = LocalSettingsCodec.parseBuiltinFlags(store.settings)

        assertNull(terminalValidation)
        assertEquals(false, flags["terminal"])
        assertNotNull(runCommandValidation)
        assertEquals("name", runCommandValidation!!.field)
        assertEquals(1, store.writeCount)
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

    private fun settingsWithUnsafeRule(): LocalSettings {
        return LocalSettingsCodec.withExecutionRules(
            LocalSettings(),
            listOf(
                ExecutionRule(
                    id = "dangerous-delete",
                    name = "危险删改",
                    enabledMode = ExecutionRuleEnabledMode.ALWAYS,
                    patterns = listOf(
                        "\\brm\\s+-rf\\b",
                        "\\brm\\s+(?=[^\\n]*--recursive\\b)(?=[^\\n]*--force\\b)[^\\n]*",
                    ),
                )
            ),
        )
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
