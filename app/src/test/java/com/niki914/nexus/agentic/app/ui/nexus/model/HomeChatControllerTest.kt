package com.niki914.nexus.agentic.app.ui.nexus.model

import com.niki914.nexus.agentic.chat.LlmStreamEvent
import com.niki914.nexus.agentic.chat.ToolCallStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class HomeChatViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun send_collectsTextAndToolCallsInStreamOrder() = runTest {
        val viewModel = HomeChatViewModel(
            streamProvider = { query ->
                assertEquals("hello", query)
                flowOf(
                    LlmStreamEvent.RoundStarted,
                    LlmStreamEvent.TextDelta(delta = "he", fullText = "he"),
                    LlmStreamEvent.ToolRunning(ToolCallStatus(name = "search", label = "Search")),
                    LlmStreamEvent.ToolSucceeded(ToolCallStatus(name = "search", label = "Search")),
                    LlmStreamEvent.TextDelta(delta = "llo", fullText = "hello"),
                    LlmStreamEvent.ToolRunning(
                        ToolCallStatus(
                            callId = "calc-1",
                            name = "calc",
                            label = "Calc"
                        )
                    ),
                    LlmStreamEvent.ToolSucceeded(
                        ToolCallStatus(
                            callId = "calc-1",
                            name = "calc",
                            label = "Calc"
                        )
                    ),
                    LlmStreamEvent.Completed(fullText = "hello back"),
                )
            },
            resetConversation = {},
        )

        viewModel.sendIntent(HomeChatIntent.InputChanged("  hello  "))
        viewModel.sendIntent(HomeChatIntent.Send)
        advanceUntilIdle()

        val state = viewModel.uiStateFlow.value
        assertEquals("", state.input)
        assertFalse(state.isGenerating)
        assertEquals(1, state.turns.size)
        val turn = state.turns.single()
        assertEquals("hello", turn.userText)
        assertEquals(
            listOf(
                HomeChatBlock.Text("he"),
                HomeChatBlock.Tool(
                    HomeToolStatus(
                        name = "Search",
                        state = HomeToolState.Succeeded
                    )
                ),
                HomeChatBlock.Text("llo"),
                HomeChatBlock.Tool(
                    HomeToolStatus(
                        callId = "calc-1",
                        name = "Calc",
                        state = HomeToolState.Succeeded
                    )
                ),
                HomeChatBlock.Text(" back"),
            ),
            turn.blocks,
        )
    }

    @Test
    fun clearConversation_clearsUiStateAndResetsRuntime() = runTest {
        var resetCalled = false
        val viewModel = HomeChatViewModel(
            streamProvider = { flowOf(LlmStreamEvent.Completed("done")) },
            resetConversation = { resetCalled = true },
        )
        viewModel.sendIntent(HomeChatIntent.InputChanged("hello"))
        viewModel.sendIntent(HomeChatIntent.Send)
        advanceUntilIdle()

        viewModel.sendIntent(HomeChatIntent.ClearConversation)
        advanceUntilIdle()

        assertTrue(resetCalled)
        val state = viewModel.uiStateFlow.value
        assertEquals("", state.input)
        assertFalse(state.isGenerating)
        assertTrue(state.turns.isEmpty())
    }

    @Test
    fun send_doesNotCreateVisibleFallbackWhenUnexpectedErrorHasNoMessage() = runTest {
        val viewModel = HomeChatViewModel(
            streamProvider = {
                flow {
                    throw RuntimeException()
                }
            },
            resetConversation = {},
        )

        viewModel.sendIntent(HomeChatIntent.InputChanged("hello"))
        viewModel.sendIntent(HomeChatIntent.Send)
        advanceUntilIdle()

        val state = viewModel.uiStateFlow.value
        assertEquals(1, state.turns.size)
        assertEquals(null, state.turns.single().errorMessage)
        assertFalse(state.isGenerating)
    }

    @Test
    fun send_ignoresSecondSendWhileGenerating() = runTest {
        val viewModel = HomeChatViewModel(
            streamProvider = {
                flow {
                    emit(LlmStreamEvent.RoundStarted)
                    awaitCancellation()
                }
            },
            resetConversation = {},
        )

        viewModel.sendIntent(HomeChatIntent.InputChanged("first"))
        viewModel.sendIntent(HomeChatIntent.Send)
        runCurrent()
        viewModel.sendIntent(HomeChatIntent.InputChanged("second"))
        viewModel.sendIntent(HomeChatIntent.Send)
        advanceUntilIdle()

        val state = viewModel.uiStateFlow.value
        assertTrue(state.isGenerating)
        assertEquals("second", state.input)
        assertEquals(1, state.turns.size)
        assertEquals("first", state.turns.single().userText)

        viewModel.sendIntent(HomeChatIntent.ClearConversation)
        advanceUntilIdle()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
