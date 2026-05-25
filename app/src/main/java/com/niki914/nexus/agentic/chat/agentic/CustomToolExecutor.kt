package com.niki914.nexus.agentic.chat.agentic

import com.niki914.nexus.agentic.chat.LocalTool
import com.niki914.nexus.h.util.xlog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.concurrent.TimeUnit

class CustomToolExecutor(
    private val timeoutMs: Long = DEFAULT_TIMEOUT_MS,
) {
    suspend fun execute(tool: LocalTool.Custom): String {
        val command = tool.command.trim()
        if (command.isBlank()) {
            return buildFailureJson(
                command = command,
                message = "Custom tool '${tool.name}' has empty command.",
            )
        }
        return withContext(Dispatchers.IO) {
            var process: Process? = null
            try {
                process = ProcessBuilder("/system/bin/sh", "-c", command)
                    .redirectErrorStream(true)
                    .start()
                val finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
                if (!finished) {
                    process.destroy()
                    process.waitFor(200, TimeUnit.MILLISECONDS)
                    if (process.isAlive) {
                        process.destroyForcibly()
                    }
                    return@withContext buildFailureJson(
                        command = command,
                        message = "Command timed out after ${timeoutMs}ms.",
                    )
                }
                val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
                val exitCode = process.exitValue()
                if (exitCode == 0) {
                    buildSuccessJson(command = command, stdout = output)
                } else {
                    buildFailureJson(
                        command = command,
                        message = output.ifBlank { "Command exited with code $exitCode." },
                    )
                }
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) {
                    process?.destroy()
                    if (process?.isAlive == true) {
                        process.destroyForcibly()
                    }
                    throw throwable
                }
                xlog("CustomToolExecutor.execute failed: ${throwable.message}")
                buildFailureJson(
                    command = command,
                    message = throwable.message ?: "Command execution failed.",
                )
            } finally {
                process?.inputStream?.close()
                process?.outputStream?.close()
                process?.errorStream?.close()
                if (process?.isAlive == true) {
                    process.destroyForcibly()
                }
            }
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
