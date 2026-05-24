package com.niki914.nexus.agentic.chat.agentic

import android.content.Context
import com.niki914.nexus.agentic.chat.LocalTool
import com.niki914.nexus.agentic.chat.ResolvedTools

class ToolCallDispatcher(
    private val builtinToolExecutor: BuiltinToolExecutor = BuiltinToolExecutor(),
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

    suspend fun executeLocalTool(
        context: Context,
        name: String,
        argumentsJson: String,
    ): String {
        val tools = currentTools()
        val builtinTool = tools
            ?.builtinTools
            .orEmpty()
            .filterIsInstance<LocalTool.Builtin>()
            .firstOrNull { it.name == name }
        if (builtinTool != null) {
            return builtinToolExecutor.execute(
                context = context,
                tool = builtinTool.tool,
                argumentsJson = argumentsJson,
            )
        }

        val commandTool = tools
            ?.customTools
            .orEmpty()
            .filterIsInstance<LocalTool.Command>()
            .firstOrNull { it.name == name }
        if (commandTool != null) {
            return executeCommandTool(commandTool)
        }

        return BuiltinToolResult.failure(
            code = "LOCAL_TOOL_NOT_EXECUTABLE",
            message = "Local tool '$name' is not executable in current runtime.",
            hint = "Check builtin_tool_flags or command_tools configuration.",
        ).toJsonString()
    }
}
