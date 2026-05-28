package com.niki914.nexus.agentic.chat.agentic.mcp

import com.niki914.nexus.agentic.chat.installRuntimeSettingsGatewayForTest
import com.niki914.nexus.agentic.runtime.settings.RuntimeEnvironment
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeMcpServer as McpServer
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeMcpTool as McpTool

class McpDiscoveryCacheStoreTest {
    @After
    fun tearDown() {
        RuntimeEnvironment.clearForTest()
    }

    @Test
    fun onToolsDiscovered_persistsParsedToolsIntoSettingsGateway() = runTest {
        val gateway = installRuntimeSettingsGatewayForTest()

        McpDiscoveryCacheStore().onToolsDiscovered(
            url = "http://127.0.0.1:51338/mcp",
            headers = mapOf("Authorization" to "Bearer token"),
            responseJson = """
                {
                  "jsonrpc": "2.0",
                  "id": 1,
                  "result": {
                    "tools": [
                      {
                        "name": "lookupSymbol",
                        "description": "Lookup symbol definition",
                        "inputSchema": {"type": "object"}
                      }
                    ]
                  }
                }
            """.trimIndent(),
        )

        assertEquals(1, gateway.writeCount)
        assertEquals(
            listOf(
                McpTool(
                    name = "lookupSymbol",
                    description = "Lookup symbol definition",
                    inputSchemaJson = """{"type":"object"}""",
                )
            ),
            gateway.listCachedTools(
                McpServer(
                    name = "aslocate",
                    url = "http://127.0.0.1:51338/mcp",
                    headers = mapOf("Authorization" to "Bearer token"),
                )
            ),
        )
    }
}
