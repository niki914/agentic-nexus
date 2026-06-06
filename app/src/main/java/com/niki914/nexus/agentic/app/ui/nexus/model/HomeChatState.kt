package com.niki914.nexus.agentic.app.ui.nexus.model

import androidx.lifecycle.viewModelScope
import com.niki914.nexus.agentic.chat.LLMController
import com.niki914.nexus.agentic.chat.LlmErrorCode
import com.niki914.nexus.agentic.chat.LlmStreamEvent
import com.niki914.nexus.cb.ComposeMVIViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

enum class HomeToolState {
    Running,
    Succeeded,
    Failed,
}

data class HomeToolStatus(
    val callId: String? = null,
    val name: String,
    val state: HomeToolState,
)

sealed interface HomeChatBlock {
    data class Text(val text: String) : HomeChatBlock
    data class Tool(val status: HomeToolStatus) : HomeChatBlock
    data class Error(val message: String, val code: LlmErrorCode? = null) : HomeChatBlock
}

data class HomeChatTurn(
    val id: Long,
    val userText: String,
    val blocks: List<HomeChatBlock> = emptyList(),
)

data class HomeChatUiState(
    val input: String = "",
    val turns: List<HomeChatTurn> = emptyList(),
    val isGenerating: Boolean = false,
    val lastEventName: String? = null,
    val streamEventCount: Int = 0,
)

sealed interface HomeChatIntent {
    data class InputChanged(val value: String) : HomeChatIntent
    data object Send : HomeChatIntent
    data object StopGenerating : HomeChatIntent
    data object ClearConversation : HomeChatIntent
}

internal interface HomeChatRuntime {
    fun stream(query: String): Flow<LlmStreamEvent>
    suspend fun resetConversation()
}

private object LlmHomeChatRuntime : HomeChatRuntime {
    override fun stream(query: String): Flow<LlmStreamEvent> = LLMController.stream(query)
    override suspend fun resetConversation() = LLMController.resetConversation()
}

class HomeChatViewModel internal constructor(
    private val runtime: HomeChatRuntime = LlmHomeChatRuntime,
) : ComposeMVIViewModel<HomeChatIntent, HomeChatUiState, Nothing>() {
    private var nextTurnId = 0L
    private var streamJob: Job? = null

    override fun initUiState(): HomeChatUiState {
        return HomeChatUiState()
    }

    override suspend fun handleIntent(intent: HomeChatIntent) {
        when (intent) {
            is HomeChatIntent.InputChanged -> onInputChanged(intent.value)
            HomeChatIntent.Send -> sendCurrentInput()
            HomeChatIntent.StopGenerating -> stopGenerating()
            HomeChatIntent.ClearConversation -> clearConversation()
        }
    }

    private fun onInputChanged(value: String) {
        updateState { copy(input = value) }
    }

    private fun sendCurrentInput() {
        val query = currentState.input.trim()
        if (query.isBlank() || currentState.isGenerating) return

        val turnId = nextTurnId++
        updateState {
            copy(
                input = "",
                turns = turns + HomeChatTurn(id = turnId, userText = query),
                isGenerating = true,
                lastEventName = null,
                streamEventCount = 0,
            )
        }

        streamJob = viewModelScope.launch {
            try {
                collectLlmStream(turnId = turnId, query = query)
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                throwable.message?.let { message ->
                    applyError(turnId = turnId, message = message, code = null)
                }
            } finally {
                if (streamJob == currentCoroutineContext()[Job]) {
                    streamJob = null
                    updateState { copy(isGenerating = false) }
                }
            }
        }
    }

    private fun stopGenerating() {
        if (!currentState.isGenerating) return
        streamJob?.cancel()
        streamJob = null
        updateState { copy(isGenerating = false) }
    }

    private fun clearConversation() {
        streamJob?.cancel()
        streamJob = null
        updateState { HomeChatUiState() }
        viewModelScope.launch {
            try {
                runtime.resetConversation()
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
            }
        }
    }

    private suspend fun collectLlmStream(turnId: Long, query: String) {
        runtime.stream(query).collect { event ->
            val eventName = eventName(event)
            val eventCount = currentState.streamEventCount + 1
            updateState {
                copy(
                    lastEventName = eventName,
                    streamEventCount = eventCount,
                )
            }
            applyEvent(turnId = turnId, event = event)
        }
    }

    private fun applyEvent(turnId: Long, event: LlmStreamEvent) {
        when (event) {
            LlmStreamEvent.RoundStarted -> updateState { copy(isGenerating = true) }
            is LlmStreamEvent.TextDelta -> updateTurn(turnId) {
                it.appendText(event.delta)
            }

            is LlmStreamEvent.ToolRunning -> updateTurn(turnId) {
                it.appendTool(event.call.callId, event.call.label, HomeToolState.Running)
            }

            is LlmStreamEvent.ToolSucceeded -> updateTurn(turnId) {
                it.updateTool(event.call.callId, event.call.label, HomeToolState.Succeeded)
            }

            is LlmStreamEvent.ToolFailed -> updateTurn(turnId) {
                it.updateTool(event.call.callId, event.call.label, HomeToolState.Failed)
            }

            is LlmStreamEvent.Error -> {
                applyError(turnId = turnId, message = event.message, code = event.code)
            }

            is LlmStreamEvent.Completed -> {
                updateTurn(turnId) {
                    it.appendFinalText(event.fullText)
                }
                updateState { copy(isGenerating = false) }
            }
        }
    }

    private fun applyError(turnId: Long, message: String, code: LlmErrorCode?) {
        updateTurn(turnId) {
            it.appendError(message, code)
        }
        updateState { copy(isGenerating = false) }
    }

    private fun updateTurn(turnId: Long, transform: (HomeChatTurn) -> HomeChatTurn) {
        val currentTurns = currentState.turns
        val index = currentTurns.indexOfFirst { it.id == turnId }
        if (index == -1) {
            return
        }
        val updatedTurn = transform(currentTurns[index])
        updateState {
            copy(turns = currentTurns.toMutableList().also { it[index] = updatedTurn })
        }
    }

    private fun HomeChatTurn.appendText(delta: String): HomeChatTurn {
        if (delta.isEmpty()) return this
        val lastBlock = blocks.lastOrNull()
        return if (lastBlock is HomeChatBlock.Text) {
            copy(blocks = blocks.dropLast(1) + lastBlock.copy(text = lastBlock.text + delta))
        } else {
            copy(blocks = blocks + HomeChatBlock.Text(delta))
        }
    }

    private fun HomeChatTurn.appendFinalText(fullText: String): HomeChatTurn {
        val displayedText =
            blocks.filterIsInstance<HomeChatBlock.Text>().joinToString(separator = "") { it.text }
        val delta = fullText.removePrefix(displayedText)
        return appendText(delta)
    }

    private fun HomeChatTurn.appendError(message: String, code: LlmErrorCode?): HomeChatTurn {
        if (message.isBlank()) return this
        return copy(blocks = blocks + HomeChatBlock.Error(message = message, code = code))
    }

    private fun HomeChatTurn.appendTool(
        callId: String?,
        label: String,
        state: HomeToolState,
    ): HomeChatTurn = copy(
        blocks = blocks + HomeChatBlock.Tool(
            HomeToolStatus(
                callId = callId,
                name = label,
                state = state,
            ),
        ),
    )

    private fun HomeChatTurn.updateTool(
        callId: String?,
        label: String,
        state: HomeToolState,
    ): HomeChatTurn {
        val index = blocks.indexOfLast { block ->
            block is HomeChatBlock.Tool && block.status.matchesTool(callId, label)
        }
        if (index == -1) {
            return appendTool(callId, label, state)
        }
        return copy(
            blocks = blocks.toMutableList().also { mutableBlocks ->
                mutableBlocks[index] = HomeChatBlock.Tool(
                    HomeToolStatus(
                        callId = callId,
                        name = label,
                        state = state,
                    ),
                )
            },
        )
    }

    private fun HomeToolStatus.matchesTool(callId: String?, label: String): Boolean {
        return if (callId != null || this.callId != null) {
            this.callId == callId
        } else {
            name == label
        }
    }

    override fun onCleared() {
        streamJob?.cancel()
        streamJob = null
        super.onCleared()
    }

    private fun eventName(event: LlmStreamEvent): String = when (event) {
        LlmStreamEvent.RoundStarted -> "RoundStarted"
        is LlmStreamEvent.TextDelta -> "TextDelta"
        is LlmStreamEvent.ToolRunning -> "ToolRunning"
        is LlmStreamEvent.ToolSucceeded -> "ToolSucceeded"
        is LlmStreamEvent.ToolFailed -> "ToolFailed"
        is LlmStreamEvent.Error -> "Error"
        is LlmStreamEvent.Completed -> "Completed"
    }
}
