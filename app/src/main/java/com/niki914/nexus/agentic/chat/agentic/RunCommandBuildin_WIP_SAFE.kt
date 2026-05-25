package com.niki914.nexus.agentic.chat.agentic

import com.niki914.s3ss10n.LocalToolConfig
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

class RunCommandBuildin_WIP_SAFE(
    private val timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    private val shellCommandRunner: ShellCommandRunner = ShellCommandRunner(),
    private val safetyPolicy: ShellCommandSafetyPolicy = ShellCommandSafetyPolicy(),
) : BuiltinTool(), RawJsonBuiltinTool {
    override val name: String = "run_command"

    override val description: String =
        "Run a command in the Android device shell (`/system/bin/sh`), not in a desktop Linux or macOS shell. Each call starts in a fresh shell and defaults to cwd='/'. The environment is minimal: many desktop tools such as apt, python, pip, node, git, or bash may be unavailable. Prefer shell builtins, common Android shell commands, and absolute device paths. Unsafe commands may be blocked by safety policy."

    override val defaultEnabled: Boolean = true

    override fun configure(config: LocalToolConfig) {
        config.description = description
        config.string("command") {
            description = "Command string to execute inside Android `/system/bin/sh -c`. Do not assume desktop package managers or scripting runtimes are available. Prefer shell builtins, Toybox or Toolbox commands, and Android-specific shell commands."
            required = true
        }
        config.string("workdir") {
            description = "Optional working directory inside the Android filesystem. Defaults to '/'. Prefer absolute device paths."
            required = false
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
        val args = try {
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
        if (args.command.isBlank()) {
            return buildResultJson(
                exitCode = INVALID_REQUEST_EXIT_CODE,
                stderr = "Field 'command' must not be blank.",
            )
        }
        val decision = safetyPolicy.evaluate(args.command)
        if (!decision.allowed) {
            return buildResultJson(
                exitCode = BLOCKED_EXIT_CODE,
                stderr = decision.reason,
            )
        }
        return executeCommand(args)
    }

    private fun parseArguments(argumentsJson: String): ParsedRunCommandArgs {
        val element = try {
            Json.parseToJsonElement(argumentsJson)
        } catch (throwable: SerializationException) {
            throw IllegalArgumentException("argumentsJson is not valid JSON.", throwable)
        } catch (throwable: IllegalArgumentException) {
            throw IllegalArgumentException("argumentsJson is not valid JSON.", throwable)
        }
        val obj = element as? JsonObject
            ?: throw IllegalArgumentException("argumentsJson must be a JSON object.")
        val command = obj["command"]?.jsonPrimitive?.contentOrNull.orEmpty().trim()
        val workdir = obj["workdir"]?.jsonPrimitive?.contentOrNull?.trim()?.ifBlank { null }
        val timeoutOverride = obj["timeout_ms"]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotBlank() }
            ?.toLongOrNull()
            ?: obj["timeout_ms"]?.jsonPrimitive?.let {
                throw IllegalArgumentException("Field 'timeout_ms' must be a positive integer.")
            }
        if (timeoutOverride != null && timeoutOverride <= 0) {
            throw IllegalArgumentException("Field 'timeout_ms' must be a positive integer.")
        }
        return ParsedRunCommandArgs(
            command = command,
            workdir = workdir,
            timeoutMs = timeoutOverride,
            mergeStderr = obj["merge_stderr"]?.jsonPrimitive?.booleanOrNull ?: false,
        )
    }

    private suspend fun executeCommand(args: ParsedRunCommandArgs): String {
        val result = shellCommandRunner.run(
            ShellCommandRequest(
                command = args.command,
                timeoutMs = args.timeoutMs ?: timeoutMs,
                workingDirectory = args.workdir?.let(::File) ?: DEFAULT_WORKING_DIRECTORY,
                mergeErrorIntoStdout = args.mergeStderr,
            )
        )
        if (result.timedOut) {
            return buildResultJson(
                exitCode = TIMEOUT_EXIT_CODE,
                stderr = result.executionErrorMessage ?: "Command timed out.",
            )
        }
        result.executionErrorMessage?.let { message ->
            return buildResultJson(
                exitCode = EXECUTION_ERROR_EXIT_CODE,
                stderr = message,
            )
        }
        return buildResultJson(
            exitCode = result.exitCode ?: EXECUTION_ERROR_EXIT_CODE,
            stdout = result.stdout.takeIf { it.isNotBlank() },
            stderr = result.stderr.takeIf { it.isNotBlank() },
        )
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

    private data class ParsedRunCommandArgs(
        val command: String,
        val workdir: String?,
        val timeoutMs: Long?,
        val mergeStderr: Boolean,
    )

    companion object {
        private const val RUN_COMMAND_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "command": {
                  "type": "string",
                  "description": "Command string to execute inside Android `/system/bin/sh -c`. Do not assume desktop package managers or scripting runtimes are available. Prefer shell builtins, Toybox or Toolbox commands, and Android-specific shell commands."
                },
                "workdir": {
                  "type": "string",
                  "description": "Optional working directory inside the Android filesystem. Defaults to '/'. Prefer absolute device paths."
                },
                "timeout_ms": {
                  "type": "integer",
                  "description": "Optional timeout override in milliseconds for the Android shell command. Must be greater than 0."
                },
                "merge_stderr": {
                  "type": "boolean",
                  "description": "Whether stderr should be merged into stdout."
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
        private val DEFAULT_WORKING_DIRECTORY = File("/")
    }
}
