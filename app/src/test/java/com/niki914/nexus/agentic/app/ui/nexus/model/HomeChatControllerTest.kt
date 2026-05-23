package com.niki914.nexus.agentic.app.ui.nexus.model

import com.niki914.nexus.agentic.chat.LlmStreamEvent
import com.niki914.nexus.agentic.chat.ToolCallStatus
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeChatControllerTest {

    @Test
    fun send_collectsTextAndUpdatesSingleToolStatus() = runBlocking {
        val controller = HomeChatController(
            streamProvider = { query ->
                assertEquals("hello", query)
                flowOf(
                    LlmStreamEvent.RoundStarted,
                    LlmStreamEvent.TextDelta(delta = "he", fullText = "he"),
                    LlmStreamEvent.ToolRunning(ToolCallStatus(name = "search", label = "Search")),
                    LlmStreamEvent.ToolSucceeded(ToolCallStatus(name = "search", label = "Search")),
                    LlmStreamEvent.Completed(fullText = "hello back"),
                )
            },
            resetConversation = {},
        )

        controller.onInputChange("  hello  ")
        controller.send(this)
        yield()

        assertEquals("", controller.input)
        assertFalse(controller.isGenerating)
        assertEquals(1, controller.turns.size)
        val turn = controller.turns.single()
        assertEquals("hello", turn.userText)
        assertEquals("hello back", turn.assistantText)
        assertEquals(HomeToolStatus(name = "Search", state = HomeToolState.Succeeded), turn.toolStatus)
    }

    @Test
    fun clearConversation_clearsUiStateAndResetsRuntime() = runBlocking {
        var resetCalled = false
        val controller = HomeChatController(
            streamProvider = { flowOf(LlmStreamEvent.Completed("done")) },
            resetConversation = { resetCalled = true },
        )
        controller.onInputChange("hello")
        controller.send(this)
        yield()

        controller.clearConversation(this)
        yield()

        assertTrue(resetCalled)
        assertEquals("", controller.input)
        assertFalse(controller.isGenerating)
        assertTrue(controller.turns.isEmpty())
    }

    @Test
    fun send_doesNotCreateVisibleFallbackWhenUnexpectedErrorHasNoMessage() = runBlocking {
        val controller = HomeChatController(
            streamProvider = {
                flow {
                    throw RuntimeException()
                }
            },
            resetConversation = {},
        )

        controller.onInputChange("hello")
        controller.send(this)
        yield()

        assertEquals(1, controller.turns.size)
        assertEquals(null, controller.turns.single().errorMessage)
        assertFalse(controller.isGenerating)
    }
}
