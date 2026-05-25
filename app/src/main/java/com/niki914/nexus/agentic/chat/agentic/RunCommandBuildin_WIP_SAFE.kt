package com.niki914.nexus.agentic.chat.agentic

import com.niki914.s3ss10n.LocalToolConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.util.concurrent.TimeUnit

class RunCommandBuildin_WIP_SAFE(
    private val timeoutMs: Long = DEFAULT_TIMEOUT_MS,
) : BuiltinTool(), RawJsonBuiltinTool {
    override val name: String = "run_command"

    override val description: String =
        "Run a shell command in a fresh /system/bin/sh process. Each call starts with a fresh environment and cwd='/' with no persisted shell state, so cd to your target directory inside the command before doing file operations. Unsafe commands are blocked."

    override fun configure(config: LocalToolConfig) {
        config.description = description
        config.string("command") {
            description = "Shell command to execute. Each call starts from cwd='/' in a fresh shell process; use cd /target/dir && ... when directory matters."
            required = true
        }
        config.rawJsonSchema(RUN_COMMAND_SCHEMA)
    }

    override suspend fun invoke(request: BuiltinToolRequest): BuiltinToolResult {
        return BuiltinToolResult.failure(
            code = "RAW_JSON_ONLY",
            message = "run_command must be executed through invokeRawJson().",
            hint = "Use BuiltinToolExecutor to execute this builtin.",
        )
    }

    override suspend fun invokeRawJson(request: BuiltinToolRequest): String {
        val command = try {
            parseArguments(request.argumentsJson)
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) {
                throw throwable
            }
            return buildResultJson(
                exitCode = INVALID_REQUEST_EXIT_CODE,
                stderr = throwable.message ?: "argumentsJson must be a JSON object with a non-blank command field.",
            )
        }
        if (command.isBlank()) {
            return buildResultJson(
                exitCode = INVALID_REQUEST_EXIT_CODE,
                stderr = "Field 'command' must not be blank.",
            )
        }
        if (isDangerousCommand(command)) {
            return buildResultJson(
                exitCode = BLOCKED_EXIT_CODE,
                stderr = "Command blocked by safety policy.",
            )
        }
        return executeCommand(command)
    }

    private fun parseArguments(argumentsJson: String): String {
        val element = try {
            Json.parseToJsonElement(argumentsJson)
        } catch (throwable: SerializationException) {
            throw IllegalArgumentException("argumentsJson is not valid JSON.", throwable)
        } catch (throwable: IllegalArgumentException) {
            throw IllegalArgumentException("argumentsJson is not valid JSON.", throwable)
        }
        val obj = element as? JsonObject
            ?: throw IllegalArgumentException("argumentsJson must be a JSON object.")
        return obj["command"]?.jsonPrimitive?.contentOrNull.orEmpty().trim()
    }

    private suspend fun executeCommand(command: String): String = withContext(Dispatchers.IO) {
        var process: Process? = null
        try {
            process = ProcessBuilder("/system/bin/sh", "-c", command)
                .directory(File("/"))
                .start()
            return@withContext coroutineScope {
                val stdoutDeferred = async { process.inputStream.bufferedReader().use { it.readText() } }
                val stderrDeferred = async { process.errorStream.bufferedReader().use { it.readText() } }
                val finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
                if (!finished) {
                    process.destroy()
                    process.waitFor(200, TimeUnit.MILLISECONDS)
                    if (process.isAlive) {
                        process.destroyForcibly()
                    }
                    stdoutDeferred.cancel()
                    stderrDeferred.cancel()
                    return@coroutineScope buildResultJson(
                        exitCode = TIMEOUT_EXIT_CODE,
                        stderr = "Command timed out after ${timeoutMs}ms.",
                    )
                }
                val exitCode = process.exitValue()
                val stdout = stdoutDeferred.await().trim()
                val stderr = stderrDeferred.await().trim()
                buildResultJson(
                    exitCode = exitCode,
                    stdout = stdout.takeIf { it.isNotBlank() },
                    stderr = stderr.takeIf { it.isNotBlank() },
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
            buildResultJson(
                exitCode = EXECUTION_ERROR_EXIT_CODE,
                stderr = throwable.message ?: "Command execution failed.",
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

    private fun buildResultJson(
        exitCode: Int,
        stdout: String? = null,
        stderr: String? = null,
    ): String {
        val payload = linkedMapOf<String, JsonPrimitive>(
            "exit_code" to JsonPrimitive(exitCode),
        )
        if (!stdout.isNullOrBlank()) {
            payload["stdout"] = JsonPrimitive(stdout)
        }
        if (!stderr.isNullOrBlank()) {
            payload["stderr"] = JsonPrimitive(stderr)
        }
        return JsonObject(payload).toString()
    }

    private fun isDangerousCommand(command: String): Boolean {
        val tokens = command.shellLikeTokens()
            .map { it.normalizedShellToken() }
            .filter { it.isNotBlank() }
        return tokens.containsDangerousCommand(depth = 0)
    }

    private fun String.shellLikeTokens(): List<String> {
        val tokens = mutableListOf<String>()
        val current = StringBuilder()
        var quote: Char? = null
        var escaped = false

        fun flush() {
            if (current.isNotEmpty()) {
                tokens += current.toString()
                current.clear()
            }
        }

        for (char in this) {
            when {
                escaped -> {
                    current.append(char)
                    escaped = false
                }
                char == '\\' -> escaped = true
                quote != null -> {
                    if (char == quote) {
                        quote = null
                    } else {
                        current.append(char)
                    }
                }
                char == '\'' || char == '"' -> quote = char
                char.isWhitespace() || char in SHELL_TOKEN_SEPARATORS -> flush()
                else -> current.append(char)
            }
        }
        if (escaped) {
            current.append('\\')
        }
        flush()
        return tokens
    }

    private fun String.normalizedShellToken(): String {
        return lowercase()
            .trim()
            .trim('"', '\'')
    }

    private fun List<String>.containsDangerousCommand(depth: Int): Boolean {
        if (depth > MAX_SHELL_PAYLOAD_DEPTH) {
            return false
        }
        for (index in indices) {
            val executable = this[index].executableName()
            if (executable in DANGEROUS_STANDALONE_COMMANDS) {
                return true
            }
            if (executable == "rm" && hasRecursiveForceRmFlags(startIndex = index + 1)) {
                return true
            }
            if (executable == "pm" && getOrNull(index + 1)?.executableName() == "uninstall") {
                return true
            }
            if (
                executable == "cmd" &&
                getOrNull(index + 1)?.executableName() == "package" &&
                getOrNull(index + 2)?.executableName() == "uninstall"
            ) {
                return true
            }
            if (containsDangerousNestedShellPayload(executable, index, depth)) {
                return true
            }
        }
        return false
    }

    private fun List<String>.containsDangerousNestedShellPayload(
        executable: String,
        index: Int,
        depth: Int,
    ): Boolean {
        val payload = when {
            executable in SHELL_COMMANDS -> shellCommandPayloadAfterC(startIndex = index + 1)
            executable == "eval" -> drop(index + 1).joinToString(" ").takeIf { it.isNotBlank() }
            else -> null
        } ?: return false

        val nestedTokens = payload.shellLikeTokens()
            .map { it.normalizedShellToken() }
            .filter { it.isNotBlank() }
        return nestedTokens.containsDangerousCommand(depth = depth + 1)
    }

    private fun List<String>.shellCommandPayloadAfterC(startIndex: Int): String? {
        for (index in startIndex until size) {
            val token = this[index]
            if (token == "-c") {
                return getOrNull(index + 1)
            }
            if (!token.startsWith("-")) {
                return null
            }
        }
        return null
    }

    private fun List<String>.hasRecursiveForceRmFlags(startIndex: Int): Boolean {
        var hasRecursive = false
        var hasForce = false
        for (index in startIndex until size) {
            val token = this[index]
            if (!token.startsWith("-")) {
                continue
            }
            when {
                token == "--recursive" -> hasRecursive = true
                token == "--force" -> hasForce = true
                token.startsWith("--") -> Unit
                else -> {
                    hasRecursive = hasRecursive || token.contains('r')
                    hasForce = hasForce || token.contains('f')
                }
            }
            if (hasRecursive && hasForce) {
                return true
            }
        }
        return false
    }

    private fun String.executableName(): String {
        return substringAfterLast('/')
    }

    companion object {
        private const val RUN_COMMAND_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "command": {
                  "type": "string",
                  "description": "Shell command to execute. Each call starts from cwd='/' in a fresh shell process; use cd /target/dir && ... when directory matters."
                }
              },
              "required": ["command"]
            }
        """
        private const val DEFAULT_TIMEOUT_MS: Long = 10_000L
        private const val INVALID_REQUEST_EXIT_CODE: Int = -1
        private const val BLOCKED_EXIT_CODE: Int = -2
        private const val TIMEOUT_EXIT_CODE: Int = 124
        private const val EXECUTION_ERROR_EXIT_CODE: Int = -3
        private const val MAX_SHELL_PAYLOAD_DEPTH = 8
        private val SHELL_TOKEN_SEPARATORS = setOf(';', '&', '|', '`', '$', '(', ')', '<', '>')
        private val SHELL_COMMANDS = setOf("sh", "bash", "mksh")
        private val DANGEROUS_STANDALONE_COMMANDS = setOf("reboot", "su", "setprop", "dd")
    }
}
