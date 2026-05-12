package com.niki914.nexus.agentic.chat

import com.niki914.nexus.agentic.mod.HookLocalSettings
import com.niki914.s3ss10n.Session
import com.niki914.s3ss10n.SessionEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object LLMController {

    enum class Pos { First, Middle, Final }

    private var session: Session? = null

    /**
     * 请求流式返回
     */
    fun send(
        query: String,
        scope: CoroutineScope,
        onChunk: suspend (String, Pos) -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            val localSettings = HookLocalSettings.refreshFromHookContext()
            obtainSession().apply {
                update {
                    endpoint = localSettings.endpoint // TODO 配置检查与快速失败
                    apiKey = localSettings.apiKey
                    model = localSettings.model
                    systemPrompt = localSettings.prompt.ifBlank { "You are a helpful assistant." }
                }
                val sb = StringBuilder()
                send(query) { event ->
                    when (event) {
                        is SessionEvent.RoundStarted -> {
                            onChunk("", Pos.First)
                        }

                        is SessionEvent.TextDelta -> { // TODO 字数测速
                            val delta = event.delta
                            sb.append(delta)
                            onChunk(sb.toString(), Pos.Middle)
                        }

                        is SessionEvent.RoundCompleted -> {
                            onChunk(sb.toString(), Pos.Final)
                        }

                        is SessionEvent.Error -> {
                            sb.append("\n[Error: ${event.message}]")
                            onChunk(sb.toString(), Pos.Middle)
                        }

                        is SessionEvent.ToolFailed -> {}
                        is SessionEvent.ToolRunning -> {}
                        is SessionEvent.ToolSucceeded -> {}
                    }
                }
            }
        }
    }

    private suspend fun obtainSession(): Session =
        session ?: Session.open {}.also { session = it }

    suspend fun resetConversation() {
        session?.resetConversation()
    }
}
