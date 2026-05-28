package com.niki914.nexus.agentic.chat.v2

import com.niki914.nexus.agentic.chat.agentic.buildin.BuiltinTool
import com.niki914.nexus.agentic.chat.agentic.buildin.BuiltinToolRegistry
import com.niki914.nexus.agentic.chat.agentic.buildin.BuiltinToolRequest
import com.niki914.nexus.agentic.chat.agentic.buildin.BuiltinToolResult
import com.niki914.nexus.agentic.chat.agentic.buildin.impl.CreateCustomToolBuiltin
import com.niki914.nexus.agentic.chat.agentic.buildin.impl.RunCommandBuildin_WIP_SAFE
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
    fun createCustomToolDescription_matchesConfigureDescription() {
        val tool = CreateCustomToolBuiltin()
        val config = LocalToolConfig()

        tool.configure(config)

        assertEquals("Create or update a custom tool setting.", tool.description)
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
    fun defaultRegistry_containsExpectedTools() {
        val registry = BuiltinToolRegistry.default()

        assertEquals(
            listOf("create_custom_tool", "notify", "run_command"),
            registry.all().map { it.name }.sorted()
        )
        assertEquals("create_custom_tool", registry.find("create_custom_tool")?.name)
        assertEquals("notify", registry.find("notify")?.name)
        assertEquals("run_command", registry.find("run_command")?.name)
    }

    @Test
    fun runCommandBuiltinDescription_matchesConfigureDescription() {
        val tool = RunCommandBuildin_WIP_SAFE()
        val config = LocalToolConfig()

        tool.configure(config)

        assertEquals(tool.description, config.description)
        assertEquals(
            "Run a command in the Android device shell (`/system/bin/sh`), not in a desktop Linux or macOS shell. Each call starts in a fresh shell and defaults to cwd='/'. The environment is minimal: many desktop tools such as apt, python, pip, node, git, or bash may be unavailable. Prefer shell builtins, common Android shell commands, and absolute device paths. Unsafe commands may be blocked by safety policy.",
            tool.description,
        )
    }

    private class FakeBuiltinTool(
        override val name: String,
        override val defaultEnabled: Boolean = false,
    ) : BuiltinTool() {
        override fun configure(config: LocalToolConfig) = Unit

        override suspend fun invoke(request: BuiltinToolRequest): BuiltinToolResult {
            return BuiltinToolResult.success(message = "ok")
        }
    }
}
