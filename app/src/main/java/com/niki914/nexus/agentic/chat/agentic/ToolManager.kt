package com.niki914.nexus.agentic.chat.agentic

import android.content.Context
import com.niki914.nexus.agentic.chat.LocalToolDefinition
import com.niki914.nexus.agentic.chat.LocalToolParameter
import com.niki914.nexus.agentic.chat.McpCachedTool
import com.niki914.nexus.agentic.chat.McpServerDefinition
import com.niki914.nexus.agentic.chat.ResolvedTools
import com.niki914.nexus.agentic.chat.ToolParameterType
import com.niki914.nexus.agentic.chat.ToolSource
import com.niki914.nexus.agentic.chat.mcpCacheKey
import com.niki914.nexus.agentic.mod.LocalSettings
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

class ToolManager {
    suspend fun resolve(
        context: Context,
        settings: LocalSettings,
    ): ResolvedTools = resolve(settings)

    fun resolve(settings: LocalSettings): ResolvedTools {
        val builtinTools = buildBuiltinTools(settings)
        val commandTools = buildCommandTools(settings)
        val customTools = buildCustomTools(settings) + commandTools
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

    private fun buildBuiltinTools(settings: LocalSettings): List<LocalToolDefinition> {
        return settings.builtinToolFlags
            .orEmpty()
            .mapNotNull { (name, value) ->
                val enabled = when (value) {
                    is JsonPrimitive -> value.booleanOrNull ?: false
                    is JsonObject -> value.boolean("enabled", default = true)
                    else -> false
                }
                if (!enabled) {
                    null
                } else {
                    LocalToolDefinition(
                        name = name,
                        description = (value as? JsonObject)?.string("description")
                            ?: "Builtin tool: $name",
                        source = ToolSource.Builtin,
                    )
                }
            }
            .sortedBy { it.name }
    }

    private fun buildCustomTools(settings: LocalSettings): List<LocalToolDefinition> {
        return settings.customTools
            .orEmptyObjects()
            .mapNotNull { obj ->
                val name = obj.string("name").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                LocalToolDefinition(
                    name = name,
                    description = obj.string("description"),
                    parameters = obj.array("parameters").orEmptyObjects()
                        .mapNotNull(::parseParameter),
                    source = ToolSource.UserDefined,
                    rawInputSchemaJson = obj.string("raw_input_schema").ifBlank { null },
                )
            }
    }

    private fun buildCommandTools(settings: LocalSettings): List<LocalToolDefinition> {
        return settings.commandTools
            .orEmptyObjects()
            .mapNotNull { obj ->
                val enabled = obj.boolean("enabled", default = true)
                val name = obj.string("name").trim()
                val command = obj.string("command").trim()
                if (!enabled || name.isBlank() || command.isBlank()) {
                    return@mapNotNull null
                }
                val description = obj.string("description").ifBlank { "Command tool: $name" }
                LocalToolDefinition(
                    name = name,
                    description = description,
                    source = ToolSource.Command,
                    command = command,
                )
            }
            .associateBy(LocalToolDefinition::name)
            .values
            .toList()
    }

    private fun parseParameter(obj: JsonObject): LocalToolParameter? {
        val name = obj.string("name").takeIf { it.isNotBlank() } ?: return null
        return LocalToolParameter(
            name = name,
            description = obj.string("description"),
            required = obj.boolean("required"),
            type = obj.string("type").toToolParameterType(),
        )
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
        builtinTools: List<LocalToolDefinition>,
        customTools: List<LocalToolDefinition>,
        mcpServers: List<McpServerDefinition>,
    ): List<String> {
        val lines = mutableListOf<String>()
        val commandToolNames = customTools
            .filter { it.source == ToolSource.Command }
            .map { it.name }
        if (commandToolNames.isNotEmpty()) {
            lines += "Available command tools: ${commandToolNames.joinToString()}"
        }

        val enabledMcpServers = mcpServers.filter { it.enabled }
        if (enabledMcpServers.isNotEmpty()) {
            val serverNames = enabledMcpServers.joinToString { it.name }
            lines += "Available MCP servers: $serverNames"
        }

        return lines
    }

    private fun String.toToolParameterType(): ToolParameterType {
        return when (lowercase()) {
            "int", "integer" -> ToolParameterType.Int
            "boolean", "bool" -> ToolParameterType.Boolean
            "number", "float", "double" -> ToolParameterType.Number
            "object" -> ToolParameterType.Object
            "array" -> ToolParameterType.Array
            else -> ToolParameterType.String
        }
    }

    private fun JsonObject?.orEmpty(): JsonObject = this ?: JsonObject(emptyMap())

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
