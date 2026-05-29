package com.niki914.nexus.agentic.chat

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LlmStreamCollectorsTest {

    @Test
    fun collectAsFull_updatesToolStatusInPlaceAndKeepsMultipleToolsSeparate() = runTest {
        val frames = mutableListOf<LlmTextFrame>()

        flowOf(
            LlmStreamEvent.RoundStarted,
            LlmStreamEvent.TextDelta(delta = "I'll call search.", fullText = "I'll call search."),
            LlmStreamEvent.ToolRunning(ToolCallStatus(callId = "search-1", name = "search")),
            LlmStreamEvent.ToolRunning(ToolCallStatus(callId = "calc-1", name = "calc")),
            LlmStreamEvent.ToolSucceeded(ToolCallStatus(callId = "search-1", name = "search")),
            LlmStreamEvent.ToolFailed(ToolCallStatus(callId = "calc-1", name = "calc"), "blocked"),
            LlmStreamEvent.TextDelta(
                delta = "I've done the check.",
                fullText = "I'll call search.I've done the check.",
            ),
            LlmStreamEvent.Completed("I'll call search.I've done the check."),
        ).collectAsFull(testLabels) { frame ->
            frames += frame
        }

        assertEquals(
            """
            I'll call search.
            `[search] success`
            `[calc] failed`
            I've done the check.
            """.trimIndent(),
            frames.last().text,
        )
        assertEquals(true, frames.first().isFirst)
        assertEquals(true, frames.last().isFinal)
    }

    @Test
    fun collectAsChunk_appendsToolStatusLinesAndPreservesAiTextOrder() = runTest {
        val frames = mutableListOf<LlmTextFrame>()

        flowOf(
            LlmStreamEvent.RoundStarted,
            LlmStreamEvent.TextDelta(delta = "I'll call search.", fullText = "I'll call search."),
            LlmStreamEvent.ToolRunning(ToolCallStatus(callId = "search-1", name = "search")),
            LlmStreamEvent.ToolSucceeded(ToolCallStatus(callId = "search-1", name = "search")),
            LlmStreamEvent.TextDelta(
                delta = "I've done the check.",
                fullText = "I'll call search.I've done the check.",
            ),
            LlmStreamEvent.Completed("I'll call search.I've done the check."),
        ).collectAsChunk(testLabels) { frame ->
            frames += frame
        }

        assertEquals(
            """
            I'll call search.
            `[search] called`
            `[search] running`
            `[search] success`
            I've done the check.
            """.trimIndent(),
            frames.last().text,
        )
        assertEquals(true, frames.first().isFirst)
        assertEquals(true, frames.last().isFinal)
    }

    @Test
    fun collectAsFull_handlesInterleavedToolStartsAndCompletions() = runTest {
        val frames = mutableListOf<LlmTextFrame>()

        flowOf(
            LlmStreamEvent.RoundStarted,
            LlmStreamEvent.TextDelta(delta = "I'll call tools.", fullText = "I'll call tools."),
            LlmStreamEvent.ToolRunning(ToolCallStatus(callId = "a-1", name = "alpha")),
            LlmStreamEvent.ToolRunning(ToolCallStatus(callId = "b-1", name = "beta")),
            LlmStreamEvent.ToolSucceeded(ToolCallStatus(callId = "b-1", name = "beta")),
            LlmStreamEvent.ToolRunning(ToolCallStatus(callId = "c-1", name = "gamma")),
            LlmStreamEvent.ToolFailed(ToolCallStatus(callId = "a-1", name = "alpha"), "timeout"),
            LlmStreamEvent.ToolSucceeded(ToolCallStatus(callId = "c-1", name = "gamma")),
            LlmStreamEvent.TextDelta(
                delta = "I've handled the crossed results.",
                fullText = "I'll call tools.I've handled the crossed results.",
            ),
            LlmStreamEvent.Completed("I'll call tools.I've handled the crossed results."),
        ).collectAsFull(testLabels) { frame ->
            frames += frame
        }

        assertEquals(
            """
            I'll call tools.
            `[alpha] failed`
            `[beta] success`
            `[gamma] success`
            I've handled the crossed results.
            """.trimIndent(),
            frames.last().text,
        )
        assertEquals(
            """
            I'll call tools.
            `[alpha] running`
            `[beta] success`
            """.trimIndent(),
            frames[6].text,
        )
        assertEquals(true, frames.first().isFirst)
        assertEquals(true, frames.last().isFinal)
    }

    @Test
    fun collectAsChunk_handlesInterleavedToolStartsAndCompletions() = runTest {
        val frames = mutableListOf<LlmTextFrame>()

        flowOf(
            LlmStreamEvent.RoundStarted,
            LlmStreamEvent.TextDelta(delta = "I'll call tools.", fullText = "I'll call tools."),
            LlmStreamEvent.ToolRunning(ToolCallStatus(callId = "a-1", name = "alpha")),
            LlmStreamEvent.ToolRunning(ToolCallStatus(callId = "b-1", name = "beta")),
            LlmStreamEvent.ToolSucceeded(ToolCallStatus(callId = "b-1", name = "beta")),
            LlmStreamEvent.ToolRunning(ToolCallStatus(callId = "c-1", name = "gamma")),
            LlmStreamEvent.ToolFailed(ToolCallStatus(callId = "a-1", name = "alpha"), "timeout"),
            LlmStreamEvent.ToolSucceeded(ToolCallStatus(callId = "c-1", name = "gamma")),
            LlmStreamEvent.TextDelta(
                delta = "I've handled the crossed results.",
                fullText = "I'll call tools.I've handled the crossed results.",
            ),
            LlmStreamEvent.Completed("I'll call tools.I've handled the crossed results."),
        ).collectAsChunk(testLabels) { frame ->
            frames += frame
        }

        assertEquals(
            """
            I'll call tools.
            `[alpha] called`
            `[alpha] running`
            `[beta] called`
            `[beta] running`
            `[beta] success`
            `[gamma] called`
            `[gamma] running`
            `[alpha] failed`
            `[gamma] success`
            I've handled the crossed results.
            """.trimIndent(),
            frames.last().text,
        )
        assertEquals(
            """
            I'll call tools.
            `[alpha] called`
            `[alpha] running`
            `[beta] called`
            `[beta] running`
            `[beta] success`
            """.trimIndent(),
            frames[6].text.trimEnd(),
        )
        assertEquals(true, frames.first().isFirst)
        assertEquals(true, frames.last().isFinal)
    }

    private val testLabels = ToolStatusLabels(
        called = "called",
        running = "running",
        success = "success",
        failed = "failed",
    )
}
