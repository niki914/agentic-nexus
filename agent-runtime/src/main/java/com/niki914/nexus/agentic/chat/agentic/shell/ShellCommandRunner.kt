package com.niki914.nexus.agentic.chat.agentic.shell

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

data class ShellCommandRequest(
    val command: String,
    val timeoutMs: Long = 10_000L,
    val workingDirectory: File? = null,
    val mergeErrorIntoStdout: Boolean = false,
)

data class ShellCommandResult(
    val exitCode: Int? = null,
    val stdout: String = "",
    val stderr: String = "",
    val timedOut: Boolean = false,
    val executionErrorMessage: String? = null,
)

class ShellCommandRunner(
    private val shellPath: String = DEFAULT_SHELL_PATH,
) {
    suspend fun run(request: ShellCommandRequest): ShellCommandResult =
        withContext(Dispatchers.IO) { // TODO 导入 cmd-android 依赖
            var process: Process? = null
            try {
                process = ProcessBuilder(shellPath, "-c", request.command)
                    .directory(request.workingDirectory)
                    .redirectErrorStream(request.mergeErrorIntoStdout)
                    .start()
                return@withContext coroutineScope {
                    val stdoutDeferred =
                        async { process.inputStream.bufferedReader().use { it.readText() } }
                    val stderrDeferred = if (request.mergeErrorIntoStdout) {
                        null
                    } else {
                        async { process.errorStream.bufferedReader().use { it.readText() } }
                    }
                    val finished = process.waitFor(request.timeoutMs, TimeUnit.MILLISECONDS)
                    if (!finished) {
                        process.destroyWithTimeout()
                        stdoutDeferred.cancel()
                        stderrDeferred?.cancel()
                        return@coroutineScope ShellCommandResult(
                            timedOut = true,
                            executionErrorMessage = "Command timed out after ${request.timeoutMs}ms.",
                        )
                    }
                    ShellCommandResult(
                        exitCode = process.exitValue(),
                        stdout = stdoutDeferred.await().trim(),
                        stderr = stderrDeferred?.await()?.trim().orEmpty(),
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
                ShellCommandResult(
                    executionErrorMessage = throwable.message ?: "Command execution failed.",
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

    private fun Process.destroyWithTimeout() {
        destroy()
        waitFor(200, TimeUnit.MILLISECONDS)
        if (isAlive) {
            destroyForcibly()
        }
    }

    companion object {
        private const val DEFAULT_SHELL_PATH = "/system/bin/sh"
        private const val DEFAULT_TIMEOUT_MS: Long = 10_000L
    }
}
