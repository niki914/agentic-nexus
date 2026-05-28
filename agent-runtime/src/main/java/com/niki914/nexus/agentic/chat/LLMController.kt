package com.niki914.nexus.agentic.chat

import com.niki914.nexus.agentic.chat.agentic.PromptComposer
import com.niki914.nexus.agentic.chat.agentic.PromptComposerInput
import com.niki914.nexus.agentic.chat.agentic.SessionToolBinder
import com.niki914.nexus.agentic.chat.agentic.ToolCallDispatcher
import com.niki914.nexus.agentic.chat.agentic.ToolManager
import com.niki914.nexus.agentic.chat.agentic.mcp.McpDiscoveryCacheStore
import com.niki914.nexus.agentic.chat.agentic.mcp.McpInterceptorHttpEngine
import com.niki914.nexus.agentic.chat.agentic.stream.LlmStreamEventMapper
import com.niki914.nexus.agentic.runtime.settings.RuntimeEnvironment
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
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeLlmConfig as LlmConfig

// TODO P1 [Parse] HTTP 400 , body={"error":{"message":"Invalid assistant message: content or toolcalls must be set","type":"invalidrequesterror","param":null,"code":"invalidrequest_error"}}
// TODO P0 PromptComposer 添加 mcp 发现状态的提示，让 Agent 知道 mcp 是失败、加载中还是可用
// TODO session 库应提供 MCP discovery 状态、cache update hook 与 tool 去重/冲突策略；当前 HttpEngine interceptor 只是 Nexus 侧 workaround
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
    private var lastMcpServersFingerprint: String? = null

    suspend fun refresh(): LlmRuntimeSnapshot {
        val previousSnapshot = runtimeState?.snapshot
        val gateway = RuntimeEnvironment.awaitSettingsGateway()
        val llmConfig = gateway.readLlmConfig()
        val mcpServers = gateway.listMcpServers()
        val customTools = gateway.listCustomTools()
        val builtinSettings = gateway.listBuiltinToolSettings()
        var resolvedTools = toolManager.resolve(
            customTools = customTools,
            mcpServers = mcpServers,
            builtinSettings = builtinSettings,
            mcpCachedTools = mcpServers.associate { server ->
                server.name to gateway.listCachedTools(server)
            },
        )
        val prompt = promptComposer.compose(
            PromptComposerInput(
                baseSystemPrompt = llmConfig.prompt,
                memorySections = buildMemorySections(llmConfig),
                toolSections = resolvedTools.promptLines,
                runtimeSections = buildRuntimeSections(llmConfig),
            )
        )
        val config = ResolvedLlmConfig(
            endpoint = llmConfig.endpoint,
            apiKey = llmConfig.apiKey,
            model = llmConfig.model,
            baseSystemPrompt = llmConfig.prompt,
            finalSystemPrompt = prompt.finalSystemPrompt,
            proxy = llmConfig.proxy,
        )
        val isNewSession = session == null
        val currentMcpServersFingerprint = gateway.fingerprintMcpServers()
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
            var refreshSucceeded = false
            try {
                val refreshResult = activeSession.refreshMcpTools()
                refreshSucceeded = refreshResult.failedServers.isEmpty()
                xlog(
                    "LLMController.refreshMcpTools refreshed=${refreshResult.refreshedServers} failed=${refreshResult.failedServers} count=${refreshResult.discoveredToolCount}"
                )
                if (refreshResult.failedServers.isNotEmpty()) {
                    gateway.clearMcpCacheByServerNames(
                        refreshResult.failedServers.map { it.serverName }.toSet()
                    )
                    resolvedTools = toolManager.resolve(
                        customTools = customTools,
                        mcpServers = mcpServers,
                        builtinSettings = builtinSettings,
                        mcpCachedTools = mcpServers.associate { server ->
                            server.name to gateway.listCachedTools(server)
                        },
                    )
                    activeSession.update {
                        applyRuntimeConfig(
                            config = config,
                            tools = resolvedTools,
                            previousTools = previousSnapshot?.tools,
                        )
                    }
                }
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

        return LlmRuntimeSnapshot(config, resolvedTools, prompt).also { snapshot ->
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
        }
    }

    private fun buildMemorySections(config: LlmConfig): List<String> =
        listOfNotNull(config.memoryPrompt.trim().takeIf { it.isNotBlank() })

    private fun buildRuntimeSections(config: LlmConfig): List<String> = emptyList()

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
