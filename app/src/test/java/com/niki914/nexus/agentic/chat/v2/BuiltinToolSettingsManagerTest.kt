package com.niki914.nexus.agentic.chat.v2

import com.niki914.nexus.agentic.chat.agentic.buildin.BuiltinToolSettingsManager
import com.niki914.nexus.agentic.mod.LocalSettings
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BuiltinToolSettingsManagerTest {
    private val manager = BuiltinToolSettingsManager()

    @Test
    fun list_defaultsMissingFlagsToEnabled() {
        val items = manager.list(LocalSettings())

        assertEquals(
            listOf("create_custom_tool", "run_command"),
            items.map { it.name }.sorted()
        )
        assertTrue(items.all { it.enabled })
    }

    @Test
    fun withEnabled_writesBooleanFlag() {
        val result = manager.withEnabled(
            settings = LocalSettings(),
            name = "create_custom_tool",
            enabled = true,
        )

        assertTrue(result.ok)
        assertEquals("OK", result.code)
        assertTrue(result.data["available_next_turn"]!!.jsonPrimitive.boolean)
        assertEquals("create_custom_tool", result.data["name"]!!.jsonPrimitive.content)
        assertTrue(result.data["enabled"]!!.jsonPrimitive.boolean)
        assertTrue(
            result.data["settings"]!!
                .jsonObject["builtin_tool_flags"]!!
                .jsonObject["create_custom_tool"]!!
                .jsonPrimitive.boolean
        )
    }

    @Test
    fun withEnabled_rejectsUnknownBuiltin() {
        val result = manager.withEnabled(
            settings = LocalSettings(),
            name = "unknown_tool",
            enabled = true,
        )

        assertFalse(result.ok)
        assertEquals("UNKNOWN_BUILTIN_TOOL", result.code)
    }

    @Test
    fun withEnabled_preservesExistingProps() {
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

        val result = manager.withEnabled(
            settings = settings,
            name = "create_custom_tool",
            enabled = true,
        )
        val updated = result.data["settings"]!!.jsonObject

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
}
