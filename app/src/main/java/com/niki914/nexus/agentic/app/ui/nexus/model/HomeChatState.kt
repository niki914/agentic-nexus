package com.niki914.nexus.agentic.app.ui.nexus.model

import androidx.lifecycle.viewModelScope
import com.niki914.nexus.agentic.chat.LLMController
import com.niki914.nexus.agentic.chat.LlmStreamEvent
import com.niki914.nexus.cb.ComposeMVIViewModel
import com.niki914.nexus.h.util.xlog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

enum class HomeToolState {
    Running,
    Succeeded,
    Failed,
}

data class HomeToolStatus(
    val name: String,
    val state: HomeToolState,
)

data class HomeChatTurn(
    val id: Long,
    val userText: String,
    val assistantText: String = "",
    val toolStatus: HomeToolStatus? = null,
    val errorMessage: String? = null,
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
    data object ClearConversation : HomeChatIntent
}

sealed interface HomeChatEffect {
    data class Error(val message: String) : HomeChatEffect
}

class HomeChatViewModel internal constructor(
    private val streamProvider: (String) -> Flow<LlmStreamEvent> = LLMController::stream,
    private val resetConversation: suspend () -> Unit = LLMController::resetConversation,
) : ComposeMVIViewModel<HomeChatIntent, HomeChatUiState, HomeChatEffect>() {
    private var nextTurnId = 0L
    private var streamJob: Job? = null

    override fun initUiState(): HomeChatUiState {
        xlog("HomeChatViewModel.Init")
        return HomeChatUiState()
    }

    override suspend fun handleIntent(intent: HomeChatIntent) {
        xlog("HomeChatViewModel.Intent type=${intent::class.simpleName}")
        when (intent) {
            is HomeChatIntent.InputChanged -> onInputChanged(intent.value)
            HomeChatIntent.Send -> sendCurrentInput()
            HomeChatIntent.ClearConversation -> clearConversation()
        }
    }

    private fun onInputChanged(value: String) {
        xlog("HomeChatViewModel.InputChanged length=${value.length}")
        updateState { copy(input = value) }
    }

    private fun sendCurrentInput() {
        val query = currentState.input.trim()
        if (query.isBlank()) {
            xlog("HomeChatViewModel.Send ignored=blank")
            return
        }
        if (currentState.isGenerating) {
            xlog("HomeChatViewModel.Send ignored=generating")
            return
        }

        val turnId = nextTurnId++
        xlog("HomeChatViewModel.Send accepted turnId=$turnId queryLength=${query.length}")
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
            xlog("HomeChatViewModel.StreamJob start turnId=$turnId")
            try {
                collectLlmStream(turnId = turnId, query = query)
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                xlog(
                    "HomeChatViewModel.StreamJob error turnId=$turnId type=${throwable::class.simpleName} message=${throwable.message}"
                )
                throwable.message?.let { message ->
                    applyError(turnId = turnId, message = message)
                }
            } finally {
                xlog("HomeChatViewModel.StreamJob finish turnId=$turnId")
                updateState { copy(isGenerating = false) }
            }
        }
    }

    private fun clearConversation() {
        xlog("HomeChatViewModel.ClearConversation start")
        streamJob?.cancel()
        streamJob = null
        updateState { HomeChatUiState() }
        viewModelScope.launch {
            try {
                resetConversation()
                xlog("HomeChatViewModel.ClearConversation resetDone")
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                xlog(
                    "HomeChatViewModel.ClearConversation resetError type=${throwable::class.simpleName} message=${throwable.message}"
                )
                throwable.message?.let { sendEffect(HomeChatEffect.Error(it)) }
            }
        }
    }

    private suspend fun collectLlmStream(turnId: Long, query: String) {
        streamProvider(query).collect { event ->
            val eventName = eventName(event)
            val eventCount = currentState.streamEventCount + 1
            xlog("HomeChatViewModel.StreamEvent turnId=$turnId type=$eventName count=$eventCount")
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
                it.copy(assistantText = event.fullText)
            }
            is LlmStreamEvent.ToolRunning -> updateTurn(turnId) {
                it.copy(toolStatus = HomeToolStatus(name = event.call.label, state = HomeToolState.Running))
            }
            is LlmStreamEvent.ToolSucceeded -> updateTurn(turnId) {
                it.copy(toolStatus = HomeToolStatus(name = event.call.label, state = HomeToolState.Succeeded))
            }
            is LlmStreamEvent.ToolFailed -> updateTurn(turnId) {
                it.copy(toolStatus = HomeToolStatus(name = event.call.label, state = HomeToolState.Failed))
            }
            is LlmStreamEvent.Error -> {
                applyError(turnId = turnId, message = event.message)
            }
            is LlmStreamEvent.Completed -> {
                updateTurn(turnId) {
                    it.copy(assistantText = event.fullText)
                }
                updateState { copy(isGenerating = false) }
            }
        }
    }

    private fun applyError(turnId: Long, message: String) {
        xlog("HomeChatViewModel.Error turnId=$turnId messageLength=${message.length}")
        updateTurn(turnId) {
            it.copy(errorMessage = message)
        }
        updateState { copy(isGenerating = false) }
        sendEffect(HomeChatEffect.Error(message))
    }

    private fun updateTurn(turnId: Long, transform: (HomeChatTurn) -> HomeChatTurn) {
        val currentTurns = currentState.turns
        val index = currentTurns.indexOfFirst { it.id == turnId }
        if (index == -1) {
            xlog("HomeChatViewModel.UpdateTurn skipped turnId=$turnId")
            return
        }
        val updatedTurn = transform(currentTurns[index])
        xlog(
            "HomeChatViewModel.UpdateTurn turnId=$turnId assistantLength=${updatedTurn.assistantText.length} tool=${updatedTurn.toolStatus?.state} hasError=${updatedTurn.errorMessage != null}"
        )
        updateState {
            copy(turns = currentTurns.toMutableList().also { it[index] = updatedTurn })
        }
    }

    override fun onCleared() {
        xlog("HomeChatViewModel.onCleared")
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
