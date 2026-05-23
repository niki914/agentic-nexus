package com.niki914.nexus.agentic.chat.v2

import com.niki914.nexus.agentic.chat.LocalTool
import com.niki914.nexus.agentic.chat.agentic.ToolManager
import com.niki914.nexus.agentic.mod.LocalSettings
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
                  "builtin_tool_flags": {"time": true, "weather": false},
                  "custom_tools": [
                    {
                      "name": "getCurrentWeather",
                      "description": "Get weather for a city",
                      "parameters": [
                        {"name": "location", "description": "City name", "required": true, "type": "string"}
                      ]
                    }
                  ],
                  "command_tools": [
                    {
                      "name": "currentTime",
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

        val resolved = ToolManager().resolve(settings)

        assertEquals(listOf("time"), resolved.builtinTools.map { it.name })
        assertTrue(resolved.builtinTools.single() is LocalTool.Builtin)

        val userDefinedTool = resolved.customTools.filterIsInstance<LocalTool.UserDefined>().single()
        assertEquals("getCurrentWeather", userDefinedTool.name)
        assertEquals("location", userDefinedTool.parameters.single().name)

        val commandTool = resolved.customTools.filterIsInstance<LocalTool.Command>().single()
        assertEquals("currentTime", commandTool.name)
        assertEquals("date +%s", commandTool.command)
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
                "Available command tools: currentTime",
                "Available MCP servers: aslocate",
            ),
            resolved.promptLines,
        )
    }
}
