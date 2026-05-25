package com.niki914.nexus.agentic.chat.v2

import com.niki914.nexus.agentic.chat.LocalTool
import com.niki914.nexus.agentic.chat.agentic.BuiltinTool
import com.niki914.nexus.agentic.chat.agentic.BuiltinToolRegistry
import com.niki914.nexus.agentic.chat.agentic.BuiltinToolRequest
import com.niki914.nexus.agentic.chat.agentic.BuiltinToolResult
import com.niki914.nexus.agentic.chat.agentic.ToolManager
import com.niki914.nexus.agentic.mod.LocalSettings
import com.niki914.s3ss10n.LocalToolConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolManagerTest {

    @Test
    fun resolveFromSettings_parsesBuiltinCustomAndMcpDefinitions() {
        val settings = LocalSettings(
            Json.parseToJsonElement(
                """
                {
                  "builtin_tool_flags": {"time": true, "weather": false, "unknown": true},
                  "custom_tools": [
                    {
                      "name": "current_time",
                      "description": "Get current timestamp",
                      "command": "date +%s",
                      "enabled": true
                    }
                  ],
                  "mcp_servers": [
                    {"name": "aslocate", "url": "http://127.0.0.1:51338/mcp"}
                  ],
                  "mcp_discovered_tools_cache": {
                    "http://127.0.0.1:51338/mcp#": {
                      "tools": [
                        {
                          "name": "lookupSymbol",
                          "description": "Lookup symbol definition",
                          "inputSchema": {"type": "object"}
                        }
                      ]
                    }
                  }
                }
                """.trimIndent()
            ).jsonObject
        )

        val resolved = ToolManager(
            builtinToolRegistry = BuiltinToolRegistry(
                listOf(FakeBuiltinTool(name = "time", description = "Read current time."))
            )
        ).resolve(settings)

        assertEquals(listOf("time"), resolved.builtinTools.map { it.name })
        assertTrue(resolved.builtinTools.single() is LocalTool.Builtin)
        assertEquals("Read current time.", resolved.builtinTools.single().description)

        val customTool = resolved.customTools.filterIsInstance<LocalTool.Custom>().single()
        assertEquals("current_time", customTool.name)
        assertEquals("date +%s", customTool.command)
        assertEquals(listOf("aslocate"), resolved.mcpServers.map { it.name })
        val mcpServer = resolved.mcpServers.single()
        val cachedTool = (mcpServer as com.niki914.nexus.agentic.chat.McpServerDefinition.Http)
            .cachedTools
            .single()
        assertEquals("lookupSymbol", cachedTool.name)
        assertEquals("Lookup symbol definition", cachedTool.description)
        assertEquals("""{"type":"object"}""", cachedTool.inputSchema.toString())
        assertEquals(
            listOf(
                "Available builtin tools: time",
                "Available custom tools: current_time",
                "Available MCP servers: aslocate",
            ),
            resolved.promptLines,
        )
    }

    @Test
    fun resolveFromSettings_defaultRegistryResolvesCreateCustomTool() {
        val settings = LocalSettings(
            Json.parseToJsonElement(
                """
                {
                  "builtin_tool_flags": {
                    "create_custom_tool": true,
                    "RunCommandBuildin_WIP_SAFE": true,
                    "unknown": true
                  }
                }
                """.trimIndent()
            ).jsonObject
        )

        val resolved = ToolManager().resolve(settings)

        assertEquals(
            listOf("RunCommandBuildin_WIP_SAFE", "create_custom_tool"),
            resolved.builtinTools.map { it.name }.sorted()
        )
        assertEquals(
            "Available builtin tools: RunCommandBuildin_WIP_SAFE, create_custom_tool",
            resolved.promptLines.first(),
        )
    }

    @Test
    fun resolveFromSettings_missingBuiltinFlagDoesNotExposeBuiltinPromptLine() {
        val settings = LocalSettings(
            Json.parseToJsonElement(
                """
                {
                  "builtin_tool_flags": {}
                }
                """.trimIndent()
            ).jsonObject
        )

        val resolved = ToolManager().resolve(settings)

        assertTrue(resolved.builtinTools.isEmpty())
        assertTrue(resolved.promptLines.none { it.startsWith("Available builtin tools:") })
    }

    private class FakeBuiltinTool(
        override val name: String,
        override val description: String = "Builtin tool: $name",
    ) : BuiltinTool() {
        override fun configure(config: LocalToolConfig) = Unit

        override suspend fun invoke(request: BuiltinToolRequest): BuiltinToolResult {
            return BuiltinToolResult.success(message = "ok")
        }
    }
}
