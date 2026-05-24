package com.niki914.nexus.agentic.chat.v2

import com.niki914.nexus.agentic.chat.agentic.BuiltinTool
import com.niki914.nexus.agentic.chat.agentic.BuiltinToolRegistry
import com.niki914.nexus.agentic.chat.agentic.BuiltinToolRequest
import com.niki914.nexus.agentic.chat.agentic.BuiltinToolResult
import com.niki914.nexus.agentic.chat.agentic.CreateCommandToolBuiltin
import com.niki914.nexus.agentic.mod.LocalSettings
import com.niki914.s3ss10n.LocalToolConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BuiltinToolTest {
    @Test
    fun defaultDescription_usesToolName() {
        val tool = FakeBuiltinTool("known")

        assertEquals("Builtin tool: known", tool.description)
    }

    @Test
    fun createCommandToolDescription_matchesConfigureDescription() {
        val tool = CreateCommandToolBuiltin()
        val config = LocalToolConfig()

        tool.configure(config)

        assertEquals("Create or update a command tool in LocalSettings.command_tools.", tool.description)
        assertEquals(tool.description, config.description)
    }

    @Test
    fun failureResult_serializesRequiredFields() {
        val result = BuiltinToolResult.failure(
            code = "INVALID_NAME",
            message = "Invalid tool name.",
            hint = "Use letters, digits, or underscores.",
            fieldErrors = mapOf("name" to "Invalid format."),
        )

        val json = Json.parseToJsonElement(result.toJsonString()).jsonObject

        assertFalse(json["ok"]!!.jsonPrimitive.content.toBoolean())
        assertEquals("INVALID_NAME", json["code"]!!.jsonPrimitive.content)
        assertEquals("Invalid tool name.", json["message"]!!.jsonPrimitive.content)
        assertEquals("Use letters, digits, or underscores.", json["hint"]!!.jsonPrimitive.content)
        assertEquals("Invalid format.", json["field_errors"]!!.jsonObject["name"]!!.jsonPrimitive.content)
        assertTrue(json["data"]!!.jsonObject.isEmpty())
    }

    @Test
    fun resolveEnabled_ignoresUnknownToolsAndReadsBooleanOrObjectFlags() {
        val registry = BuiltinToolRegistry(listOf(FakeBuiltinTool("known")))
        val settings = LocalSettings(
            Json.parseToJsonElement(
                """
                {
                  "builtin_tool_flags": {
                    "known": {"enabled": true},
                    "disabled": false,
                    "unknown": true
                  }
                }
                """.trimIndent()
            ).jsonObject
        )

        val resolved = registry.resolveEnabled(settings)

        assertEquals(listOf("known"), resolved.map { it.name })
        assertEquals("known", registry.find("known")?.name)
    }

    @Test
    fun defaultRegistry_containsCreateCommandToolDuringB03() {
        val registry = BuiltinToolRegistry.default()

        assertEquals(listOf("create_command_tool"), registry.all().map { it.name })
        assertEquals("create_command_tool", registry.find("create_command_tool")?.name)
    }

    private class FakeBuiltinTool(
        override val name: String,
    ) : BuiltinTool() {
        override fun configure(config: LocalToolConfig) = Unit

        override suspend fun invoke(request: BuiltinToolRequest): BuiltinToolResult {
            return BuiltinToolResult.success(message = "ok")
        }
    }
}
