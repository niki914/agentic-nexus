package com.niki914.nexus.agentic.mod

import com.niki914.s3ss10n.Session
import com.niki914.s3ss10n.SessionEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object LLMController {
    private var session: Session? = null
    private var sessionConfig: SessionConfig? = null

    private data class SessionConfig(
        val endpoint: String,
        val apiKey: String,
        val model: String,
        val prompt: String
    )

    /**
     * 请求流式返回
     * @param query 用户的输入
     * @param scope 协程作用域
     * @param onChunk 收到数据块的回调，参数分别为: chunkText, isFirst, isFinal
     */
    fun requestStream(
        query: String,
        scope: CoroutineScope,
        onChunk: (String, Boolean, Boolean) -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            val localSettings = HookLocalSettings.refreshFromHookContext()
            val config = SessionConfig(
                endpoint = localSettings.endpoint,
                apiKey = localSettings.apiKey,
                model = localSettings.model,
                prompt = localSettings.prompt.ifBlank { "You are a helpful assistant." }
            )
            val s = obtainSession(config)

            val sb = StringBuilder()
            s.send(query) { event ->
                when (event) {
                    is SessionEvent.RoundStarted -> {
                        onChunk("", true, false)
                    }

                    is SessionEvent.TextDelta -> {
                        val delta = event.delta
                        sb.append(delta)
                        onChunk(sb.toString(), false, false)
                    }

                    is SessionEvent.RoundCompleted -> {
                        onChunk(sb.toString(), false, true)
                    }

                    is SessionEvent.Error -> {
                        sb.append("\n[Error: ${event.message}]")
                        onChunk(sb.toString(), false, true)
                    }

                    is SessionEvent.ToolFailed -> {}
                    is SessionEvent.ToolRunning -> {}
                    is SessionEvent.ToolSucceeded -> {}
                }
            }
        }
    }

    private suspend fun obtainSession(config: SessionConfig): Session {
        val existing = session
        if (existing != null && sessionConfig == config) {
            return existing
        }
        return Session.open {
            endpoint = config.endpoint
            apiKey = config.apiKey
            model = config.model
            systemPrompt = config.prompt
        }.also {
            session = it
            sessionConfig = config
        }
    }
}
