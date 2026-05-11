package com.niki914.nexus.agentic.mod

import a0.a0.a0.a0.a0.a0.Entrance
import com.niki914.s3ss10n.Session
import com.niki914.s3ss10n.SessionEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object LLMController {
    private var session: Session? = null

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
            val s = session ?: Session.open { // TODO 疑似线程不安全
                endpoint = Entrance.LLM_BASE_URL
                apiKey = Entrance.LLM_API_KEY
                model = Entrance.LLM_MODEL_NAME
                systemPrompt = "You are a helpful assistant."
            }.also { session = it }

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
}
