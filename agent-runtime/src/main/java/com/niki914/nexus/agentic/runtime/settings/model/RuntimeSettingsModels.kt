package com.niki914.nexus.agentic.runtime.settings.model

data class RuntimeLlmConfig(
    val provider: String = "",
    val endpoint: String = "",
    val apiKey: String = "",
    val model: String = "",
    val prompt: String = "",
    val proxy: String = "",
    val memoryPrompt: String = "",
    val memories: List<String> = emptyList(),
    val takeoverKeywords: List<String> = emptyList(),
)

data class RuntimeMcpServer(
    val name: String,
    val url: String,
    val enabled: Boolean = true,
    val headers: Map<String, String> = emptyMap(),
)

data class RuntimeMcpTool(
    val name: String,
    val description: String = "",
    val inputSchemaJson: String,
)

data class RuntimeCustomTool(
    val name: String,
    val description: String,
    val command: String,
    val enabled: Boolean = true,
)

data class RuntimeBuiltinToolSetting(
    val name: String,
    val description: String,
    val enabled: Boolean,
)

data class RuntimeCustomToolValidation(
    val field: String,
    val message: String,
)
