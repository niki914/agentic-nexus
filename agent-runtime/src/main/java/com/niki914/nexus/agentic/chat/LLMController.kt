package com.niki914.nexus.agentic.chat

import com.niki914.nexus.agentic.chat.agentic.PromptComposer
import com.niki914.nexus.agentic.chat.agentic.PromptComposerInput
import com.niki914.nexus.agentic.chat.agentic.SessionToolBinder
import com.niki914.nexus.agentic.chat.agentic.ToolCallDispatcher
import com.niki914.nexus.agentic.chat.agentic.ToolManager
import com.niki914.nexus.agentic.chat.agentic.mcp.McpDiscoveryCacheStore
import com.niki914.nexus.agentic.chat.agentic.stream.LlmStreamEventMapper
import com.niki914.nexus.agentic.runtime.settings.RuntimeEnvironment
import com.niki914.nexus.agentic.runtime.settings.model.LlmApiType
import com.niki914.nexus.h.util.xlog
import com.niki914.s3ss10n.McpDiscoverySnapshot
import com.niki914.s3ss10n.McpDiscoveryState
import com.niki914.s3ss10n.McpServerDiscoverySnapshot
import com.niki914.s3ss10n.Session
import com.niki914.s3ss10n.SessionConfig
import com.niki914.s3ss10n.SessionProtocols
import com.niki914.s3ss10n.ToolCallKind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeLlmConfig as LlmConfig

object LLMController {
    private val promptComposer =
        PromptComposer()
    private val toolManager =
        ToolManager()
    private val mcpCacheStore =
        McpDiscoveryCacheStore()
    private val toolCallDispatcher =
        ToolCallDispatcher { runtimeState?.snapshot?.tools }

    private var runtimeState: RuntimeState? = null
    private var session: Session? = null
    private var sessionApiType: LlmApiType? = null
    private var lastMcpServersFingerprint: String? = null

    suspend fun refresh(): LlmRuntimeSnapshot {
        val previousSnapshot = runtimeState?.snapshot
        val gateway = RuntimeEnvironment.awaitSettingsGateway()
        val llmConfig = gateway.readLlmConfig()
        val apiType = LlmApiType.fromProvider(llmConfig.provider)
        val mcpServers = gateway.listMcpServers()
        val customTools = gateway.listCustomTools()
        val builtinSettings = gateway.listBuiltinToolSettings()
        val resolvedTools = toolManager.resolve(
            customTools = customTools,
            mcpServers = mcpServers,
            builtinSettings = builtinSettings,
            mcpCachedTools = mcpServers.associate { server ->
                server.name to gateway.listCachedTools(server)
            },
        )
        val configWithoutRuntimePrompt = ResolvedLlmConfig(
            endpoint = llmConfig.endpoint,
            apiKey = llmConfig.apiKey,
            model = llmConfig.model,
            baseSystemPrompt = llmConfig.prompt,
            finalSystemPrompt = llmConfig.prompt,
            proxy = llmConfig.proxy,
        )
        val isNewSession = session == null || sessionApiType != apiType
        val currentMcpServersFingerprint = gateway.fingerprintMcpServers()
        val activeSession = obtainSession(apiType)
        activeSession.update {
            applyRuntimeConfig(
                config = configWithoutRuntimePrompt,
                tools = resolvedTools,
                previousTools = previousSnapshot?.tools,
            )
        }
        val shouldRefreshMcp = resolvedTools.mcpServers.isNotEmpty() &&
                (isNewSession || currentMcpServersFingerprint != lastMcpServersFingerprint)
        if (shouldRefreshMcp) {
            var refreshSucceeded = false
            try {
                val refreshResult = activeSession.refreshMcpTools()
                refreshSucceeded = refreshResult.failedServers.isEmpty()
                xlog(
                    "LLMController.refreshMcpTools refreshed=${refreshResult.refreshedServers} failed=${refreshResult.failedServers} count=${refreshResult.discoveredToolCount}"
                )
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) {
                    throw throwable
                }
                xlog("LLMController.refreshMcpTools failed: ${throwable.message}")
            }
            lastMcpServersFingerprint = if (refreshSucceeded) {
                currentMcpServersFingerprint
            } else {
                null
            }
        } else {
            lastMcpServersFingerprint = currentMcpServersFingerprint
        }

        val mcpSnapshot = activeSession.getMcpDiscoverySnapshot()
        val prompt = promptComposer.compose(
            PromptComposerInput(
                baseSystemPrompt = llmConfig.prompt,
                memorySections = buildMemorySections(llmConfig),
                toolSections = resolvedTools.promptLines,
                runtimeSections = buildRuntimeSections(mcpSnapshot),
            )
        )
        val finalConfig = configWithoutRuntimePrompt.copy(finalSystemPrompt = prompt.finalSystemPrompt)
        activeSession.update {
            applyRuntimeConfig(
                config = finalConfig,
                tools = resolvedTools,
                previousTools = previousSnapshot?.tools,
            )
        }

        return LlmRuntimeSnapshot(finalConfig, resolvedTools, prompt).also { snapshot ->
            runtimeState = RuntimeState(snapshot = snapshot, session = activeSession)
        }
    }

    suspend fun refreshFromHookContext(): LlmRuntimeSnapshot = refresh()

    suspend fun snapshot(): LlmRuntimeSnapshot? = runtimeState?.snapshot

    fun stream(query: String): Flow<LlmStreamEvent> = channelFlow {
        xlog("LLMController.stream start queryLength=${query.length}")
        val state = refreshIfPossibleFromHookContext() ?: runtimeState
        if (state == null) {
            xlog("LLMController.stream runtimeNotReady")
            send(LlmStreamEvent.Error("LLM runtime is not ready"))
            return@channelFlow
        }

        xlog("LLMController.stream runtimeReady")
        val accumulator = StringBuilder()
        val startedAtMs = System.currentTimeMillis()
        val sink: SendChannel<LlmStreamEvent> = this
        try {
            xlog("LLMController.stream sessionSend begin")
            state.session.send(query).collect { event ->
                val mapped = LlmStreamEventMapper.map(event, accumulator, startedAtMs)
                xlog(
                    "LLMController.stream sessionEvent type=${event::class.simpleName} mapped=${
                        mapped?.let(
                            ::eventName
                        )
                    }"
                )
                mapped?.let { sink.send(it) }
            }
            xlog("LLMController.stream sessionSend done")
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) {
                throw throwable
            }
            xlog("LLMController.stream error type=${throwable::class.simpleName} message=${throwable.message}")
            send(
                LlmStreamEvent.Error(
                    message = throwable.message ?: "LLM stream failed",
                    throwable = throwable
                )
            )
        }
    }.flowOn(Dispatchers.IO)

    suspend fun resetConversation() {
        session?.resetConversation()
    }

    suspend fun stopCurrentRound(keepCurrentTurn: Boolean = false) {
        session?.stop(keepCurrentTurn = keepCurrentTurn)
    }

    private suspend fun refreshIfPossibleFromHookContext(): RuntimeState? {
        return try {
            refresh()
            runtimeState
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) {
                throw throwable
            }
            null
        }
    }

    private suspend fun obtainSession(apiType: LlmApiType): Session {
        session?.takeIf { sessionApiType == apiType }?.let { return it }
        session?.close()
        lastMcpServersFingerprint = null
        return openSession(apiType).also {
            session = it
            sessionApiType = apiType
        }
    }

    private suspend fun openSession(apiType: LlmApiType): Session {
        val configBlock: SessionConfig.Builder.() -> Unit = {
            mcpHooks {
                onToolsDiscovered = mcpCacheStore::onToolsDiscovered
            }
            hooks {
                when (kind) {
                    ToolCallKind.Local -> {
                        ok(
                            toolCallDispatcher.executeLocalTool(
                                name = name,
                                argumentsJson = argumentsJson,
                            )
                        )
                    }

                    is ToolCallKind.Mcp -> delegate()
                }
            }
            llmIdleTimeoutSeconds = 25
        }
        return when (apiType) {
            LlmApiType.Anthropic -> Session.open<SessionProtocols.Anthropic>(configBlock)
            LlmApiType.DeepSeek -> Session.open<SessionProtocols.DeepSeek>(configBlock)
            else -> Session.open<SessionProtocols.OpenAI>(configBlock)
        }
    }

    private fun buildMemorySections(config: LlmConfig): List<String> =
        listOfNotNull(config.memoryPrompt.trim().takeIf { it.isNotBlank() })

    private fun buildRuntimeSections(snapshot: McpDiscoverySnapshot?): List<String> {
        val servers = snapshot?.servers?.values.orEmpty()
        if (servers.isEmpty()) {
            return emptyList()
        }
        return listOf(
            servers
                .sortedBy { it.serverName }
                .joinToString(separator = "\n") { formatMcpStatusLine(it) }
        )
    }

    private fun formatMcpStatusLine(server: McpServerDiscoverySnapshot): String =
        when (server.state) {
            McpDiscoveryState.Available ->
                "${server.serverName} MCP: loaded ${server.discoveredToolCount} tools"

            McpDiscoveryState.Discovering ->
                "${server.serverName} MCP: loading"

            McpDiscoveryState.Failed ->
                "${server.serverName} MCP: load failed"

            McpDiscoveryState.UsingStaleCache ->
                "${server.serverName} MCP: using cached ${server.discoveredToolCount} tools"

            McpDiscoveryState.Idle ->
                "${server.serverName} MCP: not loaded"
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

        SessionToolBinder.run { bindTools(tools = tools, previousTools = previousTools) }
    }

    private data class RuntimeState(val snapshot: LlmRuntimeSnapshot, val session: Session)

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
