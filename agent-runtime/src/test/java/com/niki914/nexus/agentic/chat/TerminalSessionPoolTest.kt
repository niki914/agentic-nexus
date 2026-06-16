package com.niki914.nexus.agentic.chat

import com.niki914.nexus.agentic.chat.agentic.shell.TerminalAsyncReadOutcome
import com.niki914.nexus.agentic.chat.agentic.shell.TerminalAsyncStartOutcome
import com.niki914.nexus.agentic.chat.agentic.shell.TerminalCloseOutcome
import com.niki914.nexus.agentic.chat.agentic.shell.TerminalCommandOutcome
import com.niki914.nexus.agentic.chat.agentic.shell.TerminalOpenOutcome
import com.niki914.nexus.agentic.chat.agentic.shell.TerminalSessionPool
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalSessionPoolTest {
    @After
    fun tearDown() {
        runTest {
            TerminalSessionPool.closeAll()
        }
    }

    @Test
    fun openRejectsUnknownIdentityBeforeRuntimeInitialization() = runTest {
        val outcome = TerminalSessionPool.open(identity = "shizuku")

        assertTrue(outcome is TerminalOpenOutcome.InvalidRequest)
        assertEquals(
            "Field 'identity' must be one of user, root.",
            (outcome as TerminalOpenOutcome.InvalidRequest).message,
        )
    }

    @Test
    fun executeBlockingReturnsSessionNotFoundWithoutOpeningRuntime() = runTest {
        val outcome = TerminalSessionPool.executeBlocking(
            session = "user",
            command = "pwd",
            timeoutMs = 1_000L,
        )

        assertTrue(outcome is TerminalCommandOutcome.SessionNotFound)
    }

    @Test
    fun startAsyncReturnsSessionNotFoundWithoutOpeningRuntime() = runTest {
        val outcome = TerminalSessionPool.startAsync(
            session = "user",
            command = "sleep 10",
            timeoutMs = 1_000L,
        )

        assertEquals(TerminalAsyncStartOutcome.SessionNotFound("user"), outcome)
    }

    @Test
    fun readAsyncResultReturnsSessionNotFoundWithoutOpeningRuntime() = runTest {
        val outcome = TerminalSessionPool.readAsyncResult(session = "user", asyncId = "a1")

        assertEquals(TerminalAsyncReadOutcome.SessionNotFound("user"), outcome)
    }

    @Test
    fun closeIsIdempotentForMissingSession() = runTest {
        val outcome = TerminalSessionPool.close(session = "user")

        assertEquals(TerminalCloseOutcome.Closed, outcome)
    }

    @Test
    fun closeAllClearsMissingStateAndKeepsPoolReusable() = runTest {
        val first = TerminalSessionPool.closeAll()
        val second = TerminalSessionPool.closeAll()

        assertEquals(0, first.closedCount)
        assertEquals(0, second.closedCount)
        assertNull(TerminalSessionPool.get("user"))
    }
}
