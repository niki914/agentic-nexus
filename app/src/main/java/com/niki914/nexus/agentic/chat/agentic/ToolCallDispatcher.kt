package com.niki914.nexus.agentic.chat.agentic

import com.niki914.nexus.agentic.chat.LocalTool
import com.niki914.nexus.agentic.chat.ResolvedTools

class ToolCallDispatcher(
    private val commandToolExecutor: CommandToolExecutor = CommandToolExecutor(),
    private val currentTools: () -> ResolvedTools?
) {
    fun findCommandTool(name: String): LocalTool.Command? {
        return currentTools()
            ?.customTools
            .orEmpty()
            .filterIsInstance<LocalTool.Command>()
            .firstOrNull { it.name == name }
    }

    suspend fun executeCommandTool(tool: LocalTool.Command): String {
        return commandToolExecutor.execute(tool)
    }
}
