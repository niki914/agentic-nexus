package com.niki914.nexus.agentic.chat.v2

import android.content.Context
import android.content.ContextWrapper
import com.niki914.nexus.agentic.chat.agentic.buildin.BuiltinToolSettingsManager
import com.niki914.nexus.agentic.mod.LocalSettings
import com.niki914.nexus.agentic.repo.LocalSettingsStore
import com.niki914.nexus.agentic.repo.XRepo
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BuiltinToolSettingsManagerTest {
    private val context: Context = object : ContextWrapper(null) {
        override fun getApplicationContext(): Context = this
    }
    private val manager = BuiltinToolSettingsManager()

    @After
    fun tearDown() {
        XRepo.resetForTest()
    }

    @Test
    fun load_readsThroughXRepoBuiltinTools() = runTest {
        val store = FakeLocalSettingsStore(LocalSettings())
        XRepo.installStoreForTest(store)
        XRepo.init(context)

        val items = manager.load(context)

        assertEquals(
            listOf("create_custom_tool", "notify", "run_command"),
            items.map { it.name }.sorted()
        )
        assertTrue(items.all { it.enabled })
        assertEquals(0, store.writeCount)
    }

    @Test
    fun setEnabled_writesThroughXRepoBuiltinTools() = runTest {
        val store = FakeLocalSettingsStore(LocalSettings())
        XRepo.installStoreForTest(store)
        XRepo.init(context)

        val result = manager.setEnabled(
            context = context,
            name = "create_custom_tool",
            enabled = true,
        )

        assertTrue(result.ok)
        assertEquals("OK", result.code)
        assertTrue(result.data["available_next_turn"]!!.jsonPrimitive.boolean)
        assertEquals("create_custom_tool", result.data["name"]!!.jsonPrimitive.content)
        assertTrue(result.data["enabled"]!!.jsonPrimitive.boolean)
        assertEquals(1, store.writeCount)
        assertTrue(
            XRepo.builtinTools.list()
                .single { it.name == "create_custom_tool" }
                .enabled
        )
    }

    @Test
    fun setEnabled_rejectsUnknownBuiltinWithoutWriting() = runTest {
        val store = FakeLocalSettingsStore(LocalSettings())
        XRepo.installStoreForTest(store)
        XRepo.init(context)

        val result = manager.setEnabled(
            context = context,
            name = "unknown_tool",
            enabled = true,
        )

        assertFalse(result.ok)
        assertEquals("UNKNOWN_BUILTIN_TOOL", result.code)
        assertEquals(0, store.writeCount)
    }

    @Test
    fun setEnabled_preservesExistingProps() = runTest {
        val settings = LocalSettings(
            Json.parseToJsonElement(
                """
                {
                  "endpoint": "https://example.invalid",
                  "builtin_tool_flags": {
                    "create_custom_tool": {"enabled": false},
                    "legacy_builtin": true
                  }
                }
                """.trimIndent()
            ).jsonObject
        )
        val store = FakeLocalSettingsStore(settings)
        XRepo.installStoreForTest(store)
        XRepo.init(context)

        val result = manager.setEnabled(
            context = context,
            name = "create_custom_tool",
            enabled = true,
        )
        val updated = store.settings.props

        assertEquals("https://example.invalid", updated["endpoint"]!!.jsonPrimitive.content)
        assertTrue(
            updated["builtin_tool_flags"]!!
                .jsonObject["create_custom_tool"]!!
                .jsonPrimitive.boolean
        )
        assertTrue(
            updated["builtin_tool_flags"]!!
                .jsonObject["legacy_builtin"]!!
                .jsonPrimitive.boolean
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
