package com.niki914.nexus.agentic.mod

import com.niki914.s3ss10n.Session
import com.niki914.s3ss10n.SessionEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

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
        onChunk: suspend (String, Boolean, Boolean) -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            val localSettings = HookLocalSettings.refreshFromHookContext()
            val s = obtainSession()
            s.update {
                endpoint = localSettings.endpoint // TODO 配置检查与快速失败
                apiKey = localSettings.apiKey
                model = localSettings.model
                systemPrompt = localSettings.prompt.ifBlank { "You are a helpful assistant." }
            }
            val sb = StringBuilder()
            s.send(query) { event -> // EXTERNAL TODO: S3ss10m callback --> suspend
                when (event) {
                    is SessionEvent.RoundStarted -> {
                        runBlocking { onChunk("", true, false) } // EXTERNAL TODO: S3ss10m callback --> suspend
                    }

                    is SessionEvent.TextDelta -> { // TODO 字数测速
                        val delta = event.delta
                        sb.append(delta)
                        runBlocking { onChunk(sb.toString(), false, false) }
                    }

                    is SessionEvent.RoundCompleted -> {
                        runBlocking { onChunk(sb.toString(), false, true) }
                    }

                    is SessionEvent.Error -> {
                        sb.append("\n[Error: ${event.message}]")
                        runBlocking { onChunk(sb.toString(), false, true) }
                    }

                    is SessionEvent.ToolFailed -> {}
                    is SessionEvent.ToolRunning -> {}
                    is SessionEvent.ToolSucceeded -> {}
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
