package com.niki914.nexus.agentic.chat

import com.niki914.nexus.agentic.chat.agentic.shell.ShellCommandRequest
import com.niki914.nexus.agentic.chat.agentic.shell.ShellCommandRunner
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ShellCommandRunnerTest {
    private val runner = ShellCommandRunner(shellPath = "/bin/sh")

    @Test
    fun run_readsWorkingDirectoryAndSeparateStreams() = runTest {
        val dir = File(System.getProperty("java.io.tmpdir"))
        val result = runner.run(
            ShellCommandRequest(
                command = "pwd; echo err >&2",
                workingDirectory = dir,
                mergeErrorIntoStdout = false,
            )
        )

        assertEquals(0, result.exitCode)
        assertTrue(result.stdout.contains(dir.absolutePath))
        assertEquals("err", result.stderr)
    }

    @Test
    fun run_reportsTimeout() = runTest {
        val result = runner.run(
            ShellCommandRequest(
                command = "sleep 1",
                timeoutMs = 1,
            )
        )

        assertTrue(result.timedOut)
    }
}
