package com.niki914.nexus.agentic.chat.v2

import com.niki914.nexus.agentic.mod.LocalSettings

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
    val builtinTools: List<LocalToolDefinition> = emptyList(),
    val customTools: List<LocalToolDefinition> = emptyList(),
    val mcpServers: List<McpServerDefinition> = emptyList(),
    val promptLines: List<String> = emptyList(),
)

data class LocalToolDefinition(
    val name: String,
    val description: String,
    val parameters: List<LocalToolParameter> = emptyList(),
    val source: ToolSource = ToolSource.Builtin,
)

data class LocalToolParameter(
    val name: String,
    val description: String,
    val required: Boolean = false,
    val type: ToolParameterType = ToolParameterType.String,
)

enum class ToolSource {
    Builtin,
    UserDefined,
}

enum class ToolParameterType {
    String,
    Int,
    Boolean,
    Number,
}

sealed interface McpServerDefinition {
    val name: String

    data class Http(
        override val name: String,
        val url: String,
    ) : McpServerDefinition
}
