package com.niki914.nexus.agentic.chat

import com.niki914.nexus.agentic.mod.HookLocalSettings
import com.niki914.s3ss10n.Session
import com.niki914.s3ss10n.SessionEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

object LLMController {

    enum class Pos { First, Middle, Final }

    private var session: Session? = null

    fun stream(query: String): Flow<Pair<String, Pos>> = callbackFlow {
        val localSettings = HookLocalSettings.refreshFromHookContext()
        obtainSession().apply {
            update {
                endpoint = localSettings.endpoint // TODO 配置检查与快速失败
                apiKey = localSettings.apiKey
                model = localSettings.model
                systemPrompt = localSettings.prompt.ifBlank { "You are a helpful assistant." }
            }
            val sb = StringBuilder()
            send(query).collect { event ->

            }
            send(query) { event -> // TODO Session 已经支持 flow
                when (event) {
                    is SessionEvent.RoundStarted -> trySend("" to Pos.First)
                    is SessionEvent.TextDelta -> { // TODO 字数测速
                        sb.append(event.delta)
                        trySend(sb.toString() to Pos.Middle)
                    }
                    is SessionEvent.RoundCompleted -> trySend(sb.toString() to Pos.Final)
                    is SessionEvent.Error -> {
                        sb.append("\n[Error: ${event.message}]")
                        trySend(sb.toString() to Pos.Middle)
                    }
                    is SessionEvent.ToolFailed -> {}
                    is SessionEvent.ToolRunning -> {}
                    is SessionEvent.ToolSucceeded -> {}
                }
            }
        }
        awaitClose {}
    }.flowOn(Dispatchers.IO)

    private suspend fun obtainSession(): Session =
        session ?: Session.open {}.also { session = it }

    suspend fun resetConversation() {
        session?.resetConversation()
    }
}