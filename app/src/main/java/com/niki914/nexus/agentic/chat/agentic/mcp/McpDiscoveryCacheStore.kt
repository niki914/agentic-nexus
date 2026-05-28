package com.niki914.nexus.agentic.chat.agentic.mcp

import com.niki914.nexus.agentic.chat.McpCachedTool
import com.niki914.nexus.agentic.repo.McpTool
import com.niki914.nexus.agentic.repo.XRepo
import com.niki914.nexus.h.util.xTry
import com.niki914.nexus.h.util.xlog
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class McpDiscoveryCacheStore {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun onToolsDiscovered(
        url: String,
        headers: Map<String, String>,
        responseJson: String,
    ) {
        val tools =
            xTry("McpDiscoveryCacheStore.onToolsDiscovered:extract:$url") {
                extractDiscoveredTools(responseJson)
            } ?: return
        try {
            persistDiscoveredTools(url = url, headers = headers, tools = tools)
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) {
                throw throwable
            }
            xlog("McpDiscoveryCacheStore.onToolsDiscovered:persist failed for $url: ${throwable.message}")
        }
    }

    private suspend fun persistDiscoveredTools(
        url: String,
        headers: Map<String, String>,
        tools: List<McpCachedTool>,
    ) {
        XRepo.mcp.saveDiscoveredTools(
            url = url,
            headers = headers,
            tools = tools.map { tool ->
                McpTool(
                    name = tool.name,
                    description = tool.description,
                    inputSchemaJson = tool.inputSchema.toString(),
                )
            },
        )
    }

    private fun extractDiscoveredTools(responseJson: String): List<McpCachedTool> {
        val root = json.parseToJsonElement(responseJson).jsonObject
        val result = root["result"]?.jsonObject
            ?: error("MCP discovery response missing result object")
        return result["tools"]?.jsonArray.orEmpty().mapNotNull { element ->
            val tool = element as? JsonObject ?: return@mapNotNull null
            val name = tool["name"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val inputSchema = tool["inputSchema"] as? JsonObject ?: return@mapNotNull null
            if (name.isBlank()) {
                return@mapNotNull null
            }
            McpCachedTool(
                name = name,
                description = tool["description"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                inputSchema = inputSchema,
            )
        }
    }

    companion object {
        const val MCP_DISCOVERED_TOOLS_CACHE_KEY: String = "mcp_discovered_tools_cache"
    }
}
