package com.niki914.nexus.agentic.chat

import com.niki914.nexus.agentic.chat.agentic.BuiltinTool
import com.niki914.nexus.agentic.chat.agentic.PromptComposeResult
import com.niki914.nexus.agentic.mod.LocalSettings
import kotlinx.serialization.json.JsonObject

data class LlmRuntimeSnapshot(
    val settings: LocalSettings,
    val config: ResolvedLlmConfig,
    val tools: ResolvedTools,
    val prompt: PromptComposeResult,
)

data class ResolvedLlmConfig(
    val endpoint: String,
    val apiKey: String,
    val model: String,
    val baseSystemPrompt: String,
    val finalSystemPrompt: String,
    val proxy: String = "",
)

data class ResolvedTools(
    val builtinTools: List<LocalTool> = emptyList(),
    val customTools: List<LocalTool> = emptyList(),
    val mcpServers: List<McpServerDefinition> = emptyList(),
    val promptLines: List<String> = emptyList(),
)

sealed interface LocalTool {
    val name: String
    val description: String

    data class Builtin(
        override val name: String,
        override val description: String,
        val tool: BuiltinTool,
    ) : LocalTool

    data class Custom(
        override val name: String,
        override val description: String,
        val enabled: Boolean,
        val command: String,
    ) : LocalTool
}

data class LocalToolParameter(
    val name: String,
    val description: String,
    val required: Boolean = false,
    val type: ToolParameterType = ToolParameterType.String,
)

enum class ToolParameterType {
    String,
    Int,
    Boolean,
    Number,
    Object,
    Array,
}

data class McpCachedTool(
    val name: String,
    val description: String,
    val inputSchema: JsonObject,
)

fun ResolvedTools.allLocalTools(): List<LocalTool> {
    return builtinTools + customTools
}

fun ResolvedTools.allLocalToolNames(): List<String> {
    return allLocalTools().map { it.name }
}

internal fun mcpCacheKey(
    url: String,
    headers: Map<String, String>,
): String {
    val normalizedHeaders = headers
        .mapKeys { (key, _) -> key.lowercase() }
        .toSortedMap()
    return buildString {
        append(url)
        append("#")
        normalizedHeaders.forEach { (key, value) ->
            append(key)
            append("=")
            append(value)
            append("&")
        }
    }
}

sealed interface McpServerDefinition {
    val name: String
    val enabled: Boolean

    data class Http(
        override val name: String,
        val url: String,
        override val enabled: Boolean = true,
        val headers: Map<String, String> = emptyMap(),
        val cachedTools: List<McpCachedTool> = emptyList(),
    ) : McpServerDefinition
}
