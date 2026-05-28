package com.niki914.nexus.agentic.chat.agentic.custom

import com.niki914.nexus.agentic.chat.LocalTool
import com.niki914.nexus.agentic.chat.agentic.shell.ShellCommandRequest
import com.niki914.nexus.agentic.chat.agentic.shell.ShellCommandRunner
import com.niki914.nexus.agentic.chat.agentic.shell.ShellCommandSafetyPolicy
import com.niki914.nexus.h.util.xlog
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class CustomToolExecutor(
    private val timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    private val shellCommandRunner: ShellCommandRunner = ShellCommandRunner(),
    private val safetyPolicy: ShellCommandSafetyPolicy = ShellCommandSafetyPolicy(),
) {
    suspend fun execute(tool: LocalTool.Custom): String {
        val command = tool.command.trim()
        if (command.isBlank()) {
            return buildFailureJson(
                command = command,
                message = "Custom tool '${tool.name}' has empty command.",
            )
        }
        val decision = safetyPolicy.evaluate(command)
        if (!decision.allowed) {
            return buildFailureJson(
                command = command,
                message = decision.reason.ifBlank { "Command blocked by safety policy." },
            )
        }
        val result = shellCommandRunner.run(
            ShellCommandRequest(
                command = command,
                timeoutMs = timeoutMs,
                mergeErrorIntoStdout = true,
            )
        )
        result.executionErrorMessage?.let { message ->
            xlog("CustomToolExecutor.execute failed: $message")
            return buildFailureJson(command = command, message = message)
        }
        if (result.timedOut) {
            return buildFailureJson(
                command = command,
                message = "Command timed out after ${timeoutMs}ms.",
            )
        }
        val exitCode = result.exitCode ?: -1
        return if (exitCode == 0) {
            buildSuccessJson(command = command, stdout = result.stdout)
        } else {
            buildFailureJson(
                command = command,
                message = result.stdout.ifBlank { "Command exited with code $exitCode." },
            )
        }
    }

    private fun buildSuccessJson(command: String, stdout: String): String {
        return JsonObject(
            mapOf(
                "ok" to JsonPrimitive(true),
                "command" to JsonPrimitive(command),
                "stdout" to JsonPrimitive(stdout),
            )
        ).toString()
    }

    private fun buildFailureJson(command: String, message: String): String {
        return JsonObject(
            mapOf(
                "ok" to JsonPrimitive(false),
                "command" to JsonPrimitive(command),
                "message" to JsonPrimitive(message),
            )
        ).toString()
    }

    companion object {
        const val DEFAULT_TIMEOUT_MS: Long = 10_000L
    }
}
