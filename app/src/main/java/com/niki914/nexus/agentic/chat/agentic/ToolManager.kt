package com.niki914.nexus.agentic.chat.agentic

import android.content.Context
import com.niki914.nexus.agentic.chat.LocalTool
import com.niki914.nexus.agentic.chat.McpCachedTool
import com.niki914.nexus.agentic.chat.McpServerDefinition
import com.niki914.nexus.agentic.chat.ResolvedTools
import com.niki914.nexus.agentic.chat.agentic.buildin.BuiltinToolRegistry
import com.niki914.nexus.agentic.chat.mcpCacheKey
import com.niki914.nexus.agentic.mod.LocalSettings
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

class ToolManager(
    private val builtinToolRegistry: BuiltinToolRegistry = BuiltinToolRegistry.default(),
) {
    suspend fun resolve(
        context: Context,
        settings: LocalSettings,
    ): ResolvedTools = resolve(settings)

    fun resolve(settings: LocalSettings): ResolvedTools {
        val builtinTools = buildBuiltinTools(settings)
        val customTools = buildCustomTools(settings)
        val mcpServers = buildMcpServers(settings)

        return ResolvedTools(
            builtinTools = builtinTools,
            customTools = customTools,
            mcpServers = mcpServers,
            promptLines = buildPromptLines(
                builtinTools = builtinTools,
                customTools = customTools,
                mcpServers = mcpServers,
            ),
        )
    }

    private fun buildBuiltinTools(settings: LocalSettings): List<LocalTool.Builtin> {
        return builtinToolRegistry
            .resolveEnabled(settings)
            .map { tool ->
                LocalTool.Builtin(
                    name = tool.name,
                    description = tool.description,
                    tool = tool,
                )
            }
    }

    private fun buildCustomTools(settings: LocalSettings): List<LocalTool.Custom> {
        return settings.customTools
            .orEmptyObjects()
            .mapNotNull { obj ->
                val enabled = obj.boolean("enabled", default = true)
                val name = obj.string("name").trim()
                val command = obj.string("command").trim()
                if (!enabled || name.isBlank() || command.isBlank()) {
                    return@mapNotNull null
                }
                val description = obj.string("description").ifBlank { "Custom tool: $name" }
                LocalTool.Custom(
                    name = name,
                    description = description,
                    enabled = enabled,
                    command = command,
                )
            }
            .associateBy(LocalTool.Custom::name)
            .values
            .toList()
    }

    private fun buildMcpServers(settings: LocalSettings): List<McpServerDefinition> {
        val cache = settings.mcpDiscoveredToolsCache
        return settings.mcpServers
            .orEmptyObjects()
            .mapNotNull { obj ->
                val name = obj.string("name").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val url = obj.string("url").ifBlank {
                    obj.obj("transport")?.string("url").orEmpty()
                }
                if (url.isBlank()) {
                    null
                } else {
                    val headers = obj.obj("headers")
                        ?.mapValues { (_, value) -> value.jsonPrimitive.contentOrNull.orEmpty() }
                        ?: emptyMap()
                    McpServerDefinition.Http(
                        name = name,
                        url = url,
                        enabled = obj.boolean("enabled", default = true),
                        headers = headers,
                        cachedTools = parseCachedTools(
                            cache = cache?.get(
                                mcpCacheKey(
                                    url = url,
                                    headers = headers
                                )
                            ) as? JsonObject,
                        ),
                    )
                }
            }
    }

    private fun parseCachedTools(
        cache: JsonObject?,
    ): List<McpCachedTool> {
        return cache?.array("tools")
            .orEmptyObjects()
            .mapNotNull { tool ->
                val name = tool.string("name").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val inputSchema = tool.obj("inputSchema") ?: return@mapNotNull null
                McpCachedTool(
                    name = name,
                    description = tool.string("description"),
                    inputSchema = inputSchema,
                )
            }
    }

    fun buildPromptLines(
        builtinTools: List<LocalTool>,
        customTools: List<LocalTool>,
        mcpServers: List<McpServerDefinition>,
    ): List<String> {
        val lines = mutableListOf<String>()
        val builtinToolNames = builtinTools.map { it.name }
        if (builtinToolNames.isNotEmpty()) {
            lines += "Available builtin tools: ${builtinToolNames.joinToString()}"
        }

        val customToolNames = customTools
            .filterIsInstance<LocalTool.Custom>()
            .map { it.name }
        if (customToolNames.isNotEmpty()) {
            lines += "Available custom tools: ${customToolNames.joinToString()}"
        }

        val enabledMcpServers = mcpServers.filter { it.enabled }
        if (enabledMcpServers.isNotEmpty()) {
            val serverNames = enabledMcpServers.joinToString { it.name }
            lines += "Available MCP servers: $serverNames"
        }

        return lines
    }

    private fun JsonArray?.orEmptyObjects(): List<JsonObject> =
        this?.mapNotNull { it as? JsonObject } ?: emptyList()

    private fun JsonObject.string(key: String): String =
        (this[key] as? JsonPrimitive)?.contentOrNull.orEmpty()

    private fun JsonObject.boolean(key: String, default: Boolean = false): Boolean =
        (this[key] as? JsonPrimitive)?.booleanOrNull ?: default

    private fun JsonObject.array(key: String): JsonArray? =
        this[key] as? JsonArray

    private fun JsonObject.obj(key: String): JsonObject? =
        this[key] as? JsonObject
}
