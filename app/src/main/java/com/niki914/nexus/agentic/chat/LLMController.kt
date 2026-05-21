package com.niki914.nexus.agentic.chat

import android.content.Context
import com.niki914.nexus.agentic.chat.agentic.PromptComposer
import com.niki914.nexus.agentic.chat.agentic.PromptComposerInput
import com.niki914.nexus.agentic.chat.agentic.ToolManager
import com.niki914.nexus.agentic.mod.HookLocalSettings
import com.niki914.nexus.agentic.mod.LocalSettings
import com.niki914.nexus.h.util.ContextProvider
import com.niki914.s3ss10n.Session
import com.niki914.s3ss10n.SessionConfig
import com.niki914.s3ss10n.SessionEvent
import com.niki914.s3ss10n.SessionProtocols
import com.niki914.s3ss10n.ToolCallKind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

object LLMController {

    private val promptComposer = PromptComposer()
    private val toolManager = ToolManager()

    private var runtimeState: RuntimeState? = null
    private var session: Session? = null

    suspend fun refresh(context: Context): LlmRuntimeSnapshot {
        val settings = HookLocalSettings.update(context)
        val tools = toolManager.resolve(context, settings)
        val prompt = promptComposer.compose(
            PromptComposerInput(
                baseSystemPrompt = settings.prompt,
                memorySections = buildMemorySections(settings),
                toolSections = tools.promptLines,
                runtimeSections = buildRuntimeSections(settings),
            )
        )
        val config = ResolvedLlmConfig(
            endpoint = settings.endpoint,
            apiKey = settings.apiKey,
            model = settings.model,
            baseSystemPrompt = settings.prompt,
            finalSystemPrompt = prompt.finalSystemPrompt,
            proxy = settings.proxy,
        )
        val activeSession = obtainSession()
        activeSession.update {
            applyRuntimeConfig(
                config = config,
                tools = tools,
                previousTools = runtimeState?.snapshot?.tools,
            )
        }
        return LlmRuntimeSnapshot(
            settings = settings,
            config = config,
            tools = tools,
            prompt = prompt,
        ).also { snapshot ->
            runtimeState = RuntimeState(snapshot = snapshot, session = activeSession)
        }
    }

    suspend fun refreshFromHookContext(): LlmRuntimeSnapshot {
        val context = ContextProvider.await()
        return refresh(context)
    }

    suspend fun snapshot(): LlmRuntimeSnapshot? = runtimeState?.snapshot

    fun stream(query: String): Flow<LlmStreamEvent> = flow {
        val state = refreshIfPossibleFromHookContext() ?: runtimeState
        if (state == null) {
            emit(LlmStreamEvent.Error("LLM runtime is not ready"))
            return@flow
        }

        val accumulator = StringBuilder()
        val startedAtMs = System.currentTimeMillis()
        try {
            state.session.send(query).collect { event ->
                mapSessionEvent(
                    event = event,
                    accumulator = accumulator,
                    startedAtMs = startedAtMs,
                )?.let { emit(it) }
            }
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) {
                throw throwable
            }
            emit(
                LlmStreamEvent.Error(
                    message = throwable.message ?: "LLM stream failed",
                    throwable = throwable,
                )
            )
        }
    }.flowOn(Dispatchers.IO)

    suspend fun resetConversation() {
        session?.resetConversation()
    }

    private suspend fun refreshIfPossibleFromHookContext(): RuntimeState? {
        return try {
            refreshFromHookContext()
            runtimeState
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) {
                throw throwable
            }
            null
        }
    }

    private suspend fun obtainSession(): Session {
        return session ?: openSession().also { session = it }
    }

    private suspend fun openSession(): Session {
        return Session.Companion.open<SessionProtocols.OpenAI> {
            hooks {
                when (kind) {
                    ToolCallKind.Local -> error("Local tool '$name' has no executor yet.")
                    is ToolCallKind.Mcp -> delegate()
                }
            }
        }
    }

    private fun buildMemorySections(settings: LocalSettings): List<String> {
        return listOfNotNull(settings.memoryPrompt.trim().takeIf { it.isNotBlank() })
    }

    private fun buildRuntimeSections(settings: LocalSettings): List<String> {
        return emptyList()
    }

    private fun SessionConfig.Builder.applyRuntimeConfig(
        config: ResolvedLlmConfig,
        tools: ResolvedTools,
        previousTools: ResolvedTools?,
    ) {
        endpoint = config.endpoint
        apiKey = config.apiKey
        model = config.model
        systemPrompt = config.finalSystemPrompt

        localTools {
            previousTools?.allLocalToolNames().orEmpty().forEach(::remove)
        }

        mcp {
            previousTools?.mcpServers.orEmpty().forEach { remove(it.name) }
            tools.mcpServers.forEach { server ->
                when (server) {
                    is McpServerDefinition.Http -> add(server.name) {
                        enabled = server.enabled
                        headers = server.headers
                        http { url = server.url }
                    }
                }
            }
        }
    }

    private fun mapSessionEvent(
        event: SessionEvent,
        accumulator: StringBuilder,
        startedAtMs: Long,
    ): LlmStreamEvent? {
        return when (event) {
            is SessionEvent.RoundStarted -> LlmStreamEvent.RoundStarted
            is SessionEvent.TextDelta -> {
                accumulator.clear()
                accumulator.append(event.fullText)
                LlmStreamEvent.TextDelta(
                    delta = event.delta,
                    fullText = event.fullText,
                    charsPerSecond = charsPerSecond(event.fullText, startedAtMs),
                )
            }

            is SessionEvent.ToolRunning -> LlmStreamEvent.ToolRunning(event.toToolCallStatus())
            is SessionEvent.ToolSucceeded -> LlmStreamEvent.ToolSucceeded(
                call = event.toToolCallStatus(),
                outputText = event.resultJson,
            )

            is SessionEvent.ToolFailed -> LlmStreamEvent.ToolFailed(
                call = event.toToolCallStatus(),
                message = event.message,
            )

            is SessionEvent.Error -> LlmStreamEvent.Error(
                message = "[${event.stage}] ${event.message}",
                throwable = event.cause,
            )

            is SessionEvent.RoundCompleted -> {
                accumulator.clear()
                accumulator.append(event.fullText)
                LlmStreamEvent.Completed(event.fullText)
            }
        }
    }

    private fun charsPerSecond(
        fullText: String,
        startedAtMs: Long,
    ): Float {
        val elapsedMs = (System.currentTimeMillis() - startedAtMs).coerceAtLeast(1)
        return fullText.length * 1000f / elapsedMs
    }

    private fun SessionEvent.ToolRunning.toToolCallStatus(): ToolCallStatus =
        ToolCallStatus(callId = callId, name = toolName, label = toolName, kind = kind.toV2Kind())

    private fun SessionEvent.ToolSucceeded.toToolCallStatus(): ToolCallStatus =
        ToolCallStatus(callId = callId, name = toolName, label = toolName, kind = kind.toV2Kind())

    private fun SessionEvent.ToolFailed.toToolCallStatus(): ToolCallStatus =
        ToolCallStatus(callId = callId, name = toolName, label = toolName, kind = kind.toV2Kind())

    private fun ToolCallKind.toV2Kind(): com.niki914.nexus.agentic.chat.ToolCallKind {
        return when (this) {
            ToolCallKind.Local -> com.niki914.nexus.agentic.chat.ToolCallKind.Local
            is ToolCallKind.Mcp -> com.niki914.nexus.agentic.chat.ToolCallKind.Mcp
        }
    }

    private fun ResolvedTools.allLocalTools(): List<LocalToolDefinition> {
        return builtinTools + customTools
    }

    private fun ResolvedTools.allLocalToolNames(): List<String> {
        return allLocalTools().map { it.name }
    }

    private data class RuntimeState(
        val snapshot: LlmRuntimeSnapshot,
        val session: Session,
    )
}