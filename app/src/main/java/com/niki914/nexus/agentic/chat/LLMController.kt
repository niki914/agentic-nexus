package com.niki914.nexus.agentic.chat

import android.content.Context
import com.niki914.nexus.agentic.chat.agentic.LlmStreamEventMapper
import com.niki914.nexus.agentic.chat.agentic.McpDiscoveryCacheStore
import com.niki914.nexus.agentic.chat.agentic.McpInterceptorHttpEngine
import com.niki914.nexus.agentic.chat.agentic.PromptComposer
import com.niki914.nexus.agentic.chat.agentic.PromptComposerInput
import com.niki914.nexus.agentic.chat.agentic.SessionToolBinder
import com.niki914.nexus.agentic.chat.agentic.ToolCallDispatcher
import com.niki914.nexus.agentic.chat.agentic.ToolManager
import com.niki914.nexus.agentic.mod.HookLocalSettings
import com.niki914.nexus.agentic.mod.LocalSettings
import com.niki914.nexus.h.util.ContextProvider
import com.niki914.nexus.h.util.xlog
import com.niki914.s3ss10n.Session
import com.niki914.s3ss10n.SessionConfig
import com.niki914.s3ss10n.SessionProtocols
import com.niki914.s3ss10n.ToolCallKind
import com.niki914.s3ss10n.ext.net.OkHttpEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn

object LLMController {
    private val promptComposer = PromptComposer()
    private val toolManager = ToolManager()
    private val mcpCacheStore = McpDiscoveryCacheStore { sessionContext ?: ContextProvider.await() }
    private val toolCallDispatcher = ToolCallDispatcher { runtimeState?.snapshot?.tools }

    private var runtimeState: RuntimeState? = null
    private var session: Session? = null
    private var sessionContext: Context? = null
    private var lastMcpServersFingerprint: String? = null

    suspend fun refresh(context: Context): LlmRuntimeSnapshot {
        val previousSnapshot = runtimeState?.snapshot
        sessionContext = context.applicationContext
        val settings = HookLocalSettings.update(context)
        val resolvedTools = toolManager.resolve(context, settings)
        val prompt = promptComposer.compose(
            PromptComposerInput(
                baseSystemPrompt = settings.prompt,
                memorySections = buildMemorySections(settings),
                toolSections = resolvedTools.promptLines,
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
        val isNewSession = session == null
        val currentMcpServersFingerprint = settings.mcpServers?.toString().orEmpty()
        val activeSession = obtainSession()
        activeSession.update {
            applyRuntimeConfig(
                config = config,
                tools = resolvedTools,
                previousTools = previousSnapshot?.tools,
            )
        }
        val shouldRefreshMcp = resolvedTools.mcpServers.isNotEmpty() &&
                (isNewSession || currentMcpServersFingerprint != lastMcpServersFingerprint)
        if (shouldRefreshMcp) {
            try {
                val refreshResult = activeSession.refreshMcpTools()
                xlog(
                    "LLMController.refreshMcpTools refreshed=${refreshResult.refreshedServers} failed=${refreshResult.failedServers} count=${refreshResult.discoveredToolCount}"
                )
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) {
                    throw throwable
                }
                xlog("LLMController.refreshMcpTools failed: ${throwable.message}")
            }
        }
        lastMcpServersFingerprint = currentMcpServersFingerprint

        return LlmRuntimeSnapshot(settings, config, resolvedTools, prompt).also { snapshot ->
            runtimeState = RuntimeState(snapshot = snapshot, session = activeSession)
        }
    }

    suspend fun refreshFromHookContext(): LlmRuntimeSnapshot = refresh(ContextProvider.await())

    suspend fun snapshot(): LlmRuntimeSnapshot? = runtimeState?.snapshot

    fun stream(query: String): Flow<LlmStreamEvent> = channelFlow {
        val state = refreshIfPossibleFromHookContext() ?: runtimeState
        if (state == null) {
            send(LlmStreamEvent.Error("LLM runtime is not ready"))
            return@channelFlow
        }

        val accumulator = StringBuilder()
        val startedAtMs = System.currentTimeMillis()
        val sink: SendChannel<LlmStreamEvent> = this
        try {
            state.session.send(query) { event -> // TODO 代 session 修复后使用 flow api
                LlmStreamEventMapper.map(event, accumulator, startedAtMs)?.let { sink.send(it) }
            }
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) {
                throw throwable
            }
            send(
                LlmStreamEvent.Error(message = throwable.message ?: "LLM stream failed", throwable = throwable)
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

    private suspend fun obtainSession(): Session = session ?: openSession().also { session = it }

    private suspend fun openSession(): Session {
        return Session.open<SessionProtocols.OpenAI> {
            httpEngine = McpInterceptorHttpEngine(
                delegate = OkHttpEngine(),
                onToolsDiscovered = mcpCacheStore::onToolsDiscovered,
            )
            hooks {
                when (kind) {
                    ToolCallKind.Local -> {
                        val commandTool = toolCallDispatcher.findCommandTool(name)
                            ?: return@hooks error("Local tool '$name' is not executable in current runtime.")
                        ok(toolCallDispatcher.executeCommandTool(commandTool))
                    }
                    is ToolCallKind.Mcp -> delegate()
                }
            }
        }
    }

    private fun buildMemorySections(settings: LocalSettings): List<String> =
        listOfNotNull(settings.memoryPrompt.trim().takeIf { it.isNotBlank() })

    private fun buildRuntimeSections(settings: LocalSettings): List<String> = emptyList()

    private fun SessionConfig.Builder.applyRuntimeConfig(
        config: ResolvedLlmConfig,
        tools: ResolvedTools,
        previousTools: ResolvedTools?,
    ) {
        endpoint = config.endpoint
        apiKey = config.apiKey
        model = config.model
        systemPrompt = config.finalSystemPrompt

        SessionToolBinder.run { bindTools(tools = tools, previousTools = previousTools) }
    }

    private data class RuntimeState(val snapshot: LlmRuntimeSnapshot, val session: Session)
}
