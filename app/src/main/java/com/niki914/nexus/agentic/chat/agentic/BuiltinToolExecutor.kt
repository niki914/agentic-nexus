package com.niki914.nexus.agentic.chat.agentic

import android.content.Context
import kotlinx.coroutines.CancellationException

class BuiltinToolExecutor(
    private val registry: BuiltinToolRegistry = BuiltinToolRegistry.default(),
) {
    fun find(name: String): BuiltinTool? {
        return registry.find(name)
    }

    suspend fun execute(
        context: Context,
        name: String,
        argumentsJson: String,
    ): String {
        val tool = find(name)
            ?: return BuiltinToolResult.failure(
                code = "LOCAL_TOOL_NOT_EXECUTABLE",
                message = "Local tool '$name' is not executable in current runtime.",
                hint = "Check builtin_tool_flags or custom_tools configuration.",
            ).toJsonString()

        return execute(context = context, tool = tool, argumentsJson = argumentsJson)
    }

    suspend fun execute(
        context: Context,
        tool: BuiltinTool,
        argumentsJson: String,
    ): String {
        if (tool is RawJsonBuiltinTool) {
            return try {
                tool.invokeRawJson(
                    BuiltinToolRequest(
                        context = context,
                        name = tool.name,
                        argumentsJson = argumentsJson,
                    )
                )
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) {
                    throw throwable
                }
                BuiltinToolResult.failure(
                    code = "UNKNOWN_ERROR",
                    message = throwable.message ?: "Builtin tool '${tool.name}' failed.",
                    hint = "Inspect the builtin tool implementation and argumentsJson.",
                ).toJsonString()
            }
        }
        return try {
            tool.invoke(
                BuiltinToolRequest(
                    context = context,
                    name = tool.name,
                    argumentsJson = argumentsJson,
                )
            ).toJsonString()
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) {
                throw throwable
            }
            BuiltinToolResult.failure(
                code = "UNKNOWN_ERROR",
                message = throwable.message ?: "Builtin tool '${tool.name}' failed.",
                hint = "Inspect the builtin tool implementation and argumentsJson.",
            ).toJsonString()
        }
    }
}
