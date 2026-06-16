package com.niki914.nexus.agentic.chat.agentic.buildin.impl

import com.niki914.libterm.SshAuth
import com.niki914.libterm.SshHostKeyPolicy
import com.niki914.libterm.SshOpenOptions
import com.niki914.nexus.agentic.chat.agentic.buildin.BuiltinTool
import com.niki914.nexus.agentic.chat.agentic.buildin.BuiltinToolRequest
import com.niki914.nexus.agentic.chat.agentic.buildin.BuiltinToolResult
import com.niki914.nexus.agentic.chat.agentic.buildin.RawJsonBuiltinTool
import com.niki914.nexus.agentic.chat.agentic.shell.ShellCommandSafetyPolicy
import com.niki914.nexus.agentic.chat.agentic.shell.TerminalAsyncReadOutcome
import com.niki914.nexus.agentic.chat.agentic.shell.TerminalAsyncStartOutcome
import com.niki914.nexus.agentic.chat.agentic.shell.TerminalCloseOutcome
import com.niki914.nexus.agentic.chat.agentic.shell.TerminalCommandOutcome
import com.niki914.nexus.agentic.chat.agentic.shell.TerminalOpenOutcome
import com.niki914.nexus.agentic.chat.agentic.shell.TerminalSessionPool
import com.niki914.nexus.agentic.chat.agentic.shell.TerminalToolResponse
import com.niki914.s3ss10n.LocalToolConfig
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

class SshTerminalBuiltin(
    private val safetyPolicy: ShellCommandSafetyPolicy = ShellCommandSafetyPolicy(),
) : BuiltinTool(), RawJsonBuiltinTool {
    override val name: String = "ssh_terminal"

    override val description: String =
        "Manage SSH terminal sessions. For one-shot commands, prefer open_and_exec, e.g. " +
            """{"action":"open_and_exec","host":"192.168.1.10","username":"root","password":"***","command":"pwd"}. """ +
            "Use exec only with the opaque session handle returned by open or open_and_exec. " +
            "Use is_async=true only with exec for long-running commands, then poll read_async_result by async_id. " +
            "Credentials are passed for this call, should not be echoed, and should not be stored by this tool. " +
            "Private key authentication is not supported yet."

    override val defaultEnabled: Boolean = true

    override fun configure(config: LocalToolConfig) {
        config.description = description
        config.rawJsonSchema(SSH_TERMINAL_SCHEMA)
    }

    override suspend fun invoke(request: BuiltinToolRequest): BuiltinToolResult {
        return BuiltinToolResult.failure(
            code = "RAW_JSON_ONLY",
            message = "ssh_terminal accepts raw JSON requests only.",
            hint = """Use ssh_terminal with raw JSON, e.g. {"action":"open_and_exec","host":"192.168.1.10","username":"root","password":"***","command":"pwd"}""",
        )
    }

    override suspend fun invokeRawJson(request: BuiltinToolRequest): String {
        return try {
            val args = parseArguments(request.argumentsJson)
            when (args.action) {
                TerminalAction.OPEN -> handleOpen(args)
                TerminalAction.OPEN_AND_EXEC -> handleOpenAndExec(args)
                TerminalAction.EXEC -> handleExec(args)
                TerminalAction.READ_ASYNC_RESULT -> handleReadAsyncResult(args)
                TerminalAction.CLOSE -> handleClose(args)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: IllegalArgumentException) {
            TerminalToolResponse.invalidRequest(error.message ?: "Invalid ssh_terminal request.")
        } catch (error: Throwable) {
            TerminalToolResponse.internalError(error)
        }
    }

    private suspend fun handleOpen(args: SshTerminalToolArgs): String {
        val sshOptions = args.requireOpenOptions()
        return when (val outcome = TerminalSessionPool.openSsh(options = sshOptions, cwd = args.cwd)) {
            is TerminalOpenOutcome.Success -> TerminalToolResponse.openSuccess(
                session = outcome.session,
                identity = outcome.identity,
            )

            is TerminalOpenOutcome.Failure -> TerminalToolResponse.failure(
                failure = outcome.failure,
                elapsedSeconds = outcome.elapsedSeconds,
                identity = SSH_PUBLIC_IDENTITY,
            )

            is TerminalOpenOutcome.InvalidRequest -> TerminalToolResponse.invalidRequest(outcome.message)
        }
    }

    private suspend fun handleOpenAndExec(args: SshTerminalToolArgs): String {
        val sshOptions = args.requireOpenOptions()
        val command = args.requireCommand()
        val timeoutMs = args.timeoutMs ?: DEFAULT_TIMEOUT_MS
        val decision = safetyPolicy.evaluate(command)
        if (!decision.allowed) {
            return TerminalToolResponse.policyBlocked(decision)
        }

        return when (val outcome = TerminalSessionPool.openAndExecuteSsh(
            options = sshOptions,
            cwd = args.cwd,
            command = command,
            timeoutMs = timeoutMs,
        )) {
            is TerminalCommandOutcome.Success -> TerminalToolResponse.commandSuccess(
                result = outcome.result,
                elapsedSeconds = outcome.elapsedSeconds,
                session = outcome.session,
                identity = outcome.identity,
                mergeStderr = args.mergeStderr,
            )

            is TerminalCommandOutcome.Timeout -> TerminalToolResponse.commandTimeout(
                result = outcome.result,
                elapsedSeconds = outcome.elapsedSeconds,
                timeoutMs = timeoutMs,
                session = outcome.session,
                identity = outcome.identity,
                mergeStderr = args.mergeStderr,
            )

            is TerminalCommandOutcome.Failure -> TerminalToolResponse.failure(
                failure = outcome.failure,
                elapsedSeconds = outcome.elapsedSeconds,
                session = outcome.session,
                identity = outcome.identity ?: SSH_PUBLIC_IDENTITY,
            )

            is TerminalCommandOutcome.SessionNotFound -> TerminalToolResponse.sessionNotFound(outcome.session)
            is TerminalCommandOutcome.Busy -> TerminalToolResponse.sessionBusy(outcome.session, outcome.asyncId)
            is TerminalCommandOutcome.UnexpectedError -> TerminalToolResponse.internalError(
                throwable = outcome.throwable,
                elapsedSeconds = outcome.elapsedSeconds,
            )
        }
    }

    private suspend fun handleExec(args: SshTerminalToolArgs): String {
        val session = args.requireSession()
        val command = args.requireCommand()
        val timeoutMs = args.timeoutMs ?: DEFAULT_TIMEOUT_MS
        val decision = safetyPolicy.evaluate(command)
        if (!decision.allowed) {
            return TerminalToolResponse.policyBlocked(decision)
        }

        return if (args.isAsync) {
            when (val outcome = TerminalSessionPool.startAsync(
                session = session,
                command = command,
                timeoutMs = timeoutMs,
            )) {
                is TerminalAsyncStartOutcome.Accepted -> TerminalToolResponse.asyncAccepted(
                    asyncId = outcome.asyncId,
                    elapsedSeconds = outcome.elapsedSeconds,
                )

                is TerminalAsyncStartOutcome.SessionNotFound -> TerminalToolResponse.sessionNotFound(outcome.session)
                is TerminalAsyncStartOutcome.Busy -> TerminalToolResponse.sessionBusy(outcome.session, outcome.asyncId)
                is TerminalAsyncStartOutcome.InvalidRequest -> TerminalToolResponse.invalidRequest(outcome.message)
            }
        } else {
            when (val outcome = TerminalSessionPool.executeBlocking(
                session = session,
                command = command,
                timeoutMs = timeoutMs,
            )) {
                is TerminalCommandOutcome.Success -> TerminalToolResponse.commandSuccess(
                    result = outcome.result,
                    elapsedSeconds = outcome.elapsedSeconds,
                    session = outcome.session,
                    identity = outcome.identity,
                    mergeStderr = args.mergeStderr,
                )

                is TerminalCommandOutcome.Timeout -> TerminalToolResponse.commandTimeout(
                    result = outcome.result,
                    elapsedSeconds = outcome.elapsedSeconds,
                    timeoutMs = timeoutMs,
                    session = outcome.session,
                    identity = outcome.identity,
                    mergeStderr = args.mergeStderr,
                )

                is TerminalCommandOutcome.Failure -> TerminalToolResponse.failure(
                    failure = outcome.failure,
                    elapsedSeconds = outcome.elapsedSeconds,
                    session = outcome.session,
                    identity = outcome.identity,
                )

                is TerminalCommandOutcome.SessionNotFound -> TerminalToolResponse.sessionNotFound(outcome.session)
                is TerminalCommandOutcome.Busy -> TerminalToolResponse.sessionBusy(outcome.session, outcome.asyncId)
                is TerminalCommandOutcome.UnexpectedError -> TerminalToolResponse.internalError(
                    throwable = outcome.throwable,
                    elapsedSeconds = outcome.elapsedSeconds,
                )
            }
        }
    }

    private suspend fun handleReadAsyncResult(args: SshTerminalToolArgs): String {
        val session = args.requireSession()
        val asyncId = args.requireAsyncId()
        return when (val outcome = TerminalSessionPool.readAsyncResult(session = session, asyncId = asyncId)) {
            is TerminalAsyncReadOutcome.Running -> TerminalToolResponse.asyncRunning(
                stdoutPartial = outcome.stdoutPartial,
                stderrPartial = outcome.stderrPartial,
                elapsedSeconds = outcome.elapsedSeconds,
            )

            is TerminalAsyncReadOutcome.Completed -> TerminalToolResponse.commandSuccess(
                result = outcome.result,
                elapsedSeconds = outcome.elapsedSeconds,
                mergeStderr = args.mergeStderr,
            )

            is TerminalAsyncReadOutcome.TimedOut -> TerminalToolResponse.commandTimeout(
                result = outcome.result,
                elapsedSeconds = outcome.elapsedSeconds,
                timeoutMs = args.timeoutMs ?: DEFAULT_TIMEOUT_MS,
                mergeStderr = args.mergeStderr,
            )

            is TerminalAsyncReadOutcome.Failure -> TerminalToolResponse.failure(
                failure = outcome.failure,
                elapsedSeconds = outcome.elapsedSeconds,
                session = session,
            )

            is TerminalAsyncReadOutcome.AsyncNotFound -> TerminalToolResponse.asyncNotFound(
                session = outcome.session,
                asyncId = outcome.asyncId,
            )

            is TerminalAsyncReadOutcome.SessionNotFound -> TerminalToolResponse.sessionNotFound(outcome.session)
            is TerminalAsyncReadOutcome.UnexpectedError -> TerminalToolResponse.internalError(
                throwable = outcome.throwable,
                elapsedSeconds = outcome.elapsedSeconds,
            )
        }
    }

    private suspend fun handleClose(args: SshTerminalToolArgs): String {
        val session = args.requireSession()
        return when (val outcome = TerminalSessionPool.close(session = session)) {
            TerminalCloseOutcome.Closed -> TerminalToolResponse.closeSuccess()
            is TerminalCloseOutcome.UnexpectedError -> TerminalToolResponse.internalError(outcome.throwable)
        }
    }

    private fun parseArguments(argumentsJson: String): SshTerminalToolArgs {
        val element = try {
            Json.parseToJsonElement(argumentsJson)
        } catch (error: SerializationException) {
            throw IllegalArgumentException("argumentsJson is not valid JSON.", error)
        } catch (error: IllegalArgumentException) {
            throw IllegalArgumentException("argumentsJson is not valid JSON.", error)
        }
        val obj = element as? JsonObject
            ?: throw IllegalArgumentException("argumentsJson must be a JSON object.")
        obj.requireKnownKeys()
        return SshTerminalToolArgs(
            action = TerminalAction.from(obj.optionalString("action")),
            session = obj.optionalString("session")?.trim(),
            command = obj.optionalString("command"),
            cwd = obj.optionalString("cwd"),
            timeoutMs = obj.optionalLong("timeout_ms")?.also { timeoutMs ->
                require(timeoutMs > 0L) { "Field 'timeout_ms' must be greater than 0." }
            },
            mergeStderr = obj.optionalBoolean("merge_stderr") ?: false,
            isAsync = obj.optionalBoolean("is_async") ?: false,
            asyncId = obj.optionalString("async_id")?.trim(),
            host = obj.optionalString("host")?.trim(),
            port = obj.optionalLong("port")?.also { port ->
                require(port in 1L..65535L) { "Field 'port' must be in 1..65535." }
            }?.toInt() ?: SshOpenOptions.DEFAULT_PORT,
            username = obj.optionalString("username")?.trim(),
            password = obj.optionalString("password"),
            hostKeyPolicy = obj.optionalString("host_key_policy")?.trim(),
            knownHostsPath = obj.optionalString("known_hosts_path")?.trim(),
            strictHostKeyChecking = obj.optionalBoolean("strict_host_key_checking"),
            connectTimeoutMs = obj.optionalLong("connect_timeout_ms")?.also { timeoutMs ->
                require(timeoutMs > 0L) { "Field 'connect_timeout_ms' must be greater than 0." }
            }?.toInt() ?: SshOpenOptions.DEFAULT_CONNECT_TIMEOUT_MILLIS,
            serverAliveIntervalMs = obj.optionalLong("server_alive_interval_ms")?.also { intervalMs ->
                require(intervalMs >= 0L) { "Field 'server_alive_interval_ms' must be greater than or equal to 0." }
            }?.toInt() ?: SshOpenOptions.DEFAULT_SERVER_ALIVE_INTERVAL_MILLIS,
            privateKeyPem = obj.optionalString("private_key_pem"),
            passphrase = obj.optionalString("passphrase"),
        )
    }

    private fun SshTerminalToolArgs.requireOpenOptions(): SshOpenOptions {
        if (!privateKeyPem.isNullOrBlank() || !passphrase.isNullOrBlank()) {
            throw IllegalArgumentException("Private key authentication is not supported yet.")
        }
        val host = host?.takeIf(String::isNotBlank)
            ?: throw IllegalArgumentException("Field 'host' must not be blank.")
        val username = username?.takeIf(String::isNotBlank)
            ?: throw IllegalArgumentException("Field 'username' must not be blank.")
        val password = password?.takeIf(String::isNotBlank)
            ?: throw IllegalArgumentException("Field 'password' must not be blank.")
        val resolvedHostKeyPolicy = when (hostKeyPolicy?.lowercase() ?: HOST_KEY_POLICY_ACCEPT_ANY) {
            HOST_KEY_POLICY_ACCEPT_ANY -> SshHostKeyPolicy.AcceptAny
            HOST_KEY_POLICY_KNOWN_HOSTS_FILE -> {
                val path = knownHostsPath?.takeIf(String::isNotBlank)
                    ?: throw IllegalArgumentException(
                        "Field 'known_hosts_path' is required when host_key_policy is 'known_hosts_file'."
                    )
                SshHostKeyPolicy.KnownHostsFile(
                    path = path,
                    strict = strictHostKeyChecking ?: true,
                )
            }

            else -> throw IllegalArgumentException(
                "Field 'host_key_policy' must be one of accept_any, known_hosts_file."
            )
        }
        return SshOpenOptions(
            host = host,
            port = port,
            username = username,
            auth = SshAuth.Password(password),
            hostKeyPolicy = resolvedHostKeyPolicy,
            connectTimeoutMillis = connectTimeoutMs,
            serverAliveIntervalMillis = serverAliveIntervalMs,
        )
    }

    private fun SshTerminalToolArgs.requireSession(): String {
        return session?.takeIf(String::isNotBlank)
            ?: throw IllegalArgumentException("Field 'session' is required for action '${action.wireName()}'.")
    }

    private fun SshTerminalToolArgs.requireCommand(): String {
        return command?.takeIf(String::isNotBlank)
            ?: throw IllegalArgumentException("Field 'command' must not be blank.")
    }

    private fun SshTerminalToolArgs.requireAsyncId(): String {
        return asyncId?.takeIf(String::isNotBlank)
            ?: throw IllegalArgumentException("Field 'async_id' is required for action 'read_async_result'.")
    }

    private fun TerminalAction.wireName(): String {
        return when (this) {
            TerminalAction.OPEN -> "open"
            TerminalAction.OPEN_AND_EXEC -> "open_and_exec"
            TerminalAction.EXEC -> "exec"
            TerminalAction.READ_ASYNC_RESULT -> "read_async_result"
            TerminalAction.CLOSE -> "close"
        }
    }

    private fun JsonObject.requireKnownKeys() {
        val unknownKeys = keys - REQUEST_KEYS
        if (unknownKeys.isNotEmpty()) {
            throw IllegalArgumentException(
                "Unknown ssh_terminal request field(s): ${unknownKeys.sorted().joinToString()}."
            )
        }
    }

    private fun JsonObject.optionalString(key: String): String? {
        val element = this[key] ?: return null
        if (element is JsonNull) {
            return null
        }
        val primitive = element.asPrimitive(key)
        return try {
            Json.decodeFromJsonElement<String>(primitive)
        } catch (error: SerializationException) {
            throw IllegalArgumentException("Field '$key' must be a string.")
        }
    }

    private fun JsonObject.optionalBoolean(key: String): Boolean? {
        val element = this[key] ?: return null
        if (element is JsonNull) {
            return null
        }
        return element.asPrimitive(key).booleanOrNull
            ?: throw IllegalArgumentException("Field '$key' must be a boolean.")
    }

    private fun JsonObject.optionalLong(key: String): Long? {
        val element = this[key] ?: return null
        if (element is JsonNull) {
            return null
        }
        return element.asPrimitive(key).longOrNull
            ?: throw IllegalArgumentException("Field '$key' must be an integer.")
    }

    private fun JsonElement.asPrimitive(key: String): JsonPrimitive {
        return runCatching { jsonPrimitive }.getOrElse {
            throw IllegalArgumentException("Field '$key' must be a primitive value.")
        }
    }

    private data class SshTerminalToolArgs(
        val action: TerminalAction,
        val session: String?,
        val command: String?,
        val cwd: String?,
        val timeoutMs: Long?,
        val mergeStderr: Boolean,
        val isAsync: Boolean,
        val asyncId: String?,
        val host: String?,
        val port: Int,
        val username: String?,
        val password: String?,
        val hostKeyPolicy: String?,
        val knownHostsPath: String?,
        val strictHostKeyChecking: Boolean?,
        val connectTimeoutMs: Int,
        val serverAliveIntervalMs: Int,
        val privateKeyPem: String?,
        val passphrase: String?,
    )

    companion object {
        private const val DEFAULT_TIMEOUT_MS = 30_000L
        private const val SSH_PUBLIC_IDENTITY = "ssh"
        private const val HOST_KEY_POLICY_ACCEPT_ANY = "accept_any"
        private const val HOST_KEY_POLICY_KNOWN_HOSTS_FILE = "known_hosts_file"
        private val REQUEST_KEYS = setOf(
            "action",
            "session",
            "command",
            "cwd",
            "timeout_ms",
            "merge_stderr",
            "is_async",
            "async_id",
            "host",
            "port",
            "username",
            "password",
            "host_key_policy",
            "known_hosts_path",
            "strict_host_key_checking",
            "connect_timeout_ms",
            "server_alive_interval_ms",
            "private_key_pem",
            "passphrase",
        )
        private const val SSH_TERMINAL_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "action": {
                  "type": "string",
                  "enum": ["open", "open_and_exec", "exec", "read_async_result", "close"]
                },
                "session": {
                  "type": "string",
                  "description": "Opaque session handle returned by open or open_and_exec."
                },
                "command": {
                  "type": "string",
                  "description": "Remote shell command. Required for open_and_exec and exec."
                },
                "cwd": {
                  "type": "string",
                  "description": "Optional working directory used only when opening a new SSH session."
                },
                "timeout_ms": {
                  "type": "integer",
                  "minimum": 1
                },
                "merge_stderr": {
                  "type": "boolean"
                },
                "is_async": {
                  "type": "boolean"
                },
                "async_id": {
                  "type": "string"
                },
                "host": {
                  "type": "string"
                },
                "port": {
                  "type": "integer",
                  "minimum": 1,
                  "maximum": 65535
                },
                "username": {
                  "type": "string"
                },
                "password": {
                  "type": "string"
                },
                "host_key_policy": {
                  "type": "string",
                  "enum": ["accept_any", "known_hosts_file"]
                },
                "known_hosts_path": {
                  "type": "string"
                },
                "strict_host_key_checking": {
                  "type": "boolean"
                },
                "connect_timeout_ms": {
                  "type": "integer",
                  "minimum": 1
                },
                "server_alive_interval_ms": {
                  "type": "integer",
                  "minimum": 0
                },
                "private_key_pem": {
                  "type": "string",
                  "description": "Reserved for future use. Currently unsupported."
                },
                "passphrase": {
                  "type": "string",
                  "description": "Reserved for future use. Currently unsupported."
                }
              },
              "required": ["action"]
            }
        """
    }
}
