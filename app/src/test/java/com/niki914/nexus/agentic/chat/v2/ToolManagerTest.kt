package com.niki914.nexus.agentic.chat.v2

import com.niki914.nexus.agentic.chat.agentic.ToolManager
import com.niki914.nexus.agentic.mod.LocalSettings
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
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
                  "mcp_servers": [
                    {"name": "aslocate", "url": "http://127.0.0.1:51338/mcp"}
                  ]
                }
                """.trimIndent()
            ).jsonObject
        )

        val resolved = ToolManager().resolve(settings)

        assertEquals(listOf("time"), resolved.builtinTools.map { it.name })
        assertEquals(listOf("getCurrentWeather"), resolved.customTools.map { it.name })
        assertEquals("location", resolved.customTools.single().parameters.single().name)
        assertEquals(listOf("aslocate"), resolved.mcpServers.map { it.name })
        assertEquals(
            listOf("Available MCP servers: aslocate"),
            resolved.promptLines,
        )
    }
}
