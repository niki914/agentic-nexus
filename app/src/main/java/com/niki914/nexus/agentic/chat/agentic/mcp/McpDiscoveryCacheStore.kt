package com.niki914.nexus.agentic.chat.agentic.mcp

import android.content.Context
import com.niki914.nexus.agentic.chat.McpCachedTool
import com.niki914.nexus.agentic.chat.mcpCacheKey
import com.niki914.nexus.agentic.mod.LocalSettings
import com.niki914.nexus.agentic.mod.XService
import com.niki914.nexus.h.util.ContextProvider
import com.niki914.nexus.h.util.xTry
import com.niki914.nexus.h.util.xlog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class McpDiscoveryCacheStore(
    private val contextProvider: suspend () -> Context = { ContextProvider.await() },
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val writeMutex = Mutex()

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
        val context = contextProvider()
        writeMutex.withLock {
            val latestSettings = XService.getLocalSettings(context)
            val latestCache =
                latestSettings.mcpDiscoveredToolsCache?.toMutableMap() ?: mutableMapOf()
            latestCache[mcpCacheKey(url = url, headers = headers)] = buildMcpCacheEntry(tools)

            val updatedProps = latestSettings.props.toMutableMap()
            updatedProps[MCP_DISCOVERED_TOOLS_CACHE_KEY] = JsonObject(latestCache)
            XService.putLocalSettings(context, LocalSettings(JsonObject(updatedProps)))
        }
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

    private fun buildMcpCacheEntry(tools: List<McpCachedTool>): JsonObject {
        return JsonObject(
            mapOf(
                "tools" to JsonArray(
                    tools.map { tool ->
                        JsonObject(
                            mapOf(
                                "name" to JsonPrimitive(tool.name),
                                "description" to JsonPrimitive(tool.description),
                                "inputSchema" to tool.inputSchema,
                            )
                        )
                    }
                ),
            )
        )
    }

    companion object {
        const val MCP_DISCOVERED_TOOLS_CACHE_KEY: String = "mcp_discovered_tools_cache"
    }
}
