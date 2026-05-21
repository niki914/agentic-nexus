package com.niki914.nexus.agentic.chat.v2

import android.content.Context
import com.niki914.nexus.agentic.mod.LocalSettings

class ToolManager {

    suspend fun resolve(
        context: Context,
        settings: LocalSettings,
    ): ResolvedTools {
        val builtinTools = buildBuiltinTools(settings)

        // TODO 从 XService / LocalSettings 读取用户自定义 tool 与 MCP 配置。
        // 当前先只保留 API 骨架与内建 tool 的落点。
        val customTools = emptyList<LocalToolDefinition>()
        val mcpServers = emptyList<McpServerDefinition>()

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

    private suspend fun buildBuiltinTools(settings: LocalSettings): List<LocalToolDefinition> {
        // TODO 接入真实内建 tool 开关；当前保留静态骨架。
        return emptyList()
    }

    fun buildPromptLines(
        builtinTools: List<LocalToolDefinition>,
        customTools: List<LocalToolDefinition>,
        mcpServers: List<McpServerDefinition>,
    ): List<String> {
        val lines = mutableListOf<String>()

        if (builtinTools.isNotEmpty() || customTools.isNotEmpty()) {
            val allTools = (builtinTools + customTools).joinToString { it.name }
            lines += "Available local tools: $allTools"
        }

        if (mcpServers.isNotEmpty()) {
            val serverNames = mcpServers.joinToString { it.name }
            lines += "Available MCP servers: $serverNames"
        }

        return lines
    }
}
