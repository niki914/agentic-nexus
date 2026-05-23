package com.niki914.nexus.agentic.app.ui.nexus.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.niki914.nexus.agentic.chat.LLMController
import com.niki914.nexus.agentic.chat.LlmStreamEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
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

@Composable
fun rememberHomeChatController(): HomeChatController = remember {
    HomeChatController()
}

class HomeChatController internal constructor(
    private val streamProvider: (String) -> Flow<LlmStreamEvent> = LLMController::stream,
    private val resetConversation: suspend () -> Unit = LLMController::resetConversation,
) {
    val turns = mutableStateListOf<HomeChatTurn>()

    var input by mutableStateOf("")
        private set

    var isGenerating by mutableStateOf(false)
        private set

    private var nextTurnId = 0L
    private var streamJob: Job? = null

    fun onInputChange(value: String) {
        input = value
    }

    fun send(scope: CoroutineScope) {
        val query = input.trim()
        if (query.isBlank() || isGenerating) return

        val turnId = nextTurnId++
        turns += HomeChatTurn(id = turnId, userText = query)
        input = ""
        isGenerating = true

        streamJob = scope.launch {
            try {
                collectLlmStream(turnId = turnId, query = query)
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                throwable.message?.let { message ->
                    applyError(turnId = turnId, message = message)
                }
            } finally {
                isGenerating = false
            }
        }
    }

    fun clearConversation(scope: CoroutineScope) {
        streamJob?.cancel()
        streamJob = null
        turns.clear()
        input = ""
        isGenerating = false
        scope.launch {
            resetConversation()
        }
    }

    private suspend fun collectLlmStream(turnId: Long, query: String) {
        streamProvider(query).collect { event ->
            applyEvent(turnId = turnId, event = event)
        }
    }

    private fun applyEvent(turnId: Long, event: LlmStreamEvent) {
        when (event) {
            LlmStreamEvent.RoundStarted -> isGenerating = true
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
                isGenerating = false
            }
        }
    }

    private fun applyError(turnId: Long, message: String) {
        updateTurn(turnId) {
            it.copy(errorMessage = message)
        }
        isGenerating = false
    }

    private fun updateTurn(turnId: Long, transform: (HomeChatTurn) -> HomeChatTurn) {
        val index = turns.indexOfFirst { it.id == turnId }
        if (index != -1) {
            turns[index] = transform(turns[index])
        }
    }
}
