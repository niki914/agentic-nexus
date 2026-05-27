package com.niki914.nexus.agentic.repo

data class LlmConfig(
    val provider: String = "",
    val endpoint: String = "",
    val apiKey: String = "",
    val model: String = "",
    val prompt: String = "",
    val proxy: String = "",
    val memoryPrompt: String = "",
    val takeoverKeywords: List<String> = emptyList(),
)

data class McpServer(
    val name: String,
    val url: String,
    val enabled: Boolean = true,
    val headers: Map<String, String> = emptyMap(),
)

data class McpTool(
    val name: String,
    val description: String = "",
    val inputSchemaJson: String,
)

data class CustomTool(
    val name: String,
    val description: String,
    val command: String,
    val enabled: Boolean = true,
)

data class BuiltinToolSetting(
    val name: String,
    val description: String,
    val enabled: Boolean,
)

data class CustomToolValidation(
    val field: String,
    val message: String,
)
