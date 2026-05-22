package com.niki914.nexus.agentic.chat

import android.content.Context
import com.niki914.nexus.agentic.chat.agentic.McpInterceptorHttpEngine
import com.niki914.nexus.agentic.chat.agentic.PromptComposer
import com.niki914.nexus.agentic.chat.agentic.PromptComposerInput
import com.niki914.nexus.agentic.chat.agentic.ToolManager
import com.niki914.nexus.agentic.mod.HookLocalSettings
import com.niki914.nexus.agentic.mod.LocalSettings
import com.niki914.nexus.agentic.mod.XService
import com.niki914.nexus.h.util.ContextProvider
import com.niki914.nexus.h.util.xTry
import com.niki914.nexus.h.util.xlog
import com.niki914.s3ss10n.Session
import com.niki914.s3ss10n.SessionConfig
import com.niki914.s3ss10n.SessionEvent
import com.niki914.s3ss10n.SessionProtocols
import com.niki914.s3ss10n.ToolCallKind
import com.niki914.s3ss10n.ext.net.OkHttpEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object LLMController {
    private const val MCP_DISCOVERED_TOOLS_CACHE_KEY = "mcp_discovered_tools_cache"

    private val json = Json { ignoreUnknownKeys = true }

    private val promptComposer = PromptComposer()
    private val toolManager = ToolManager()
    private val mcpCacheWriteMutex = Mutex()

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

        return LlmRuntimeSnapshot(
            settings = settings,
            config = config,
            tools = resolvedTools,
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
                mapSessionEvent(
                    event = event,
                    accumulator = accumulator,
                    startedAtMs = startedAtMs,
                )?.let { sink.send(it) }
            }
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) {
                throw throwable
            }
            send(
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
        return Session.open<SessionProtocols.OpenAI> {
            httpEngine = McpInterceptorHttpEngine(
                delegate = OkHttpEngine(),
                onToolsDiscovered = ::handleMcpDiscoveryResponse,
            )
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
                        server.cachedTools.forEach { cachedTool ->
                            tool(cachedTool.name) {
                                description = cachedTool.description
                                rawJsonSchema(cachedTool.inputSchema.toString())
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun handleMcpDiscoveryResponse(
        url: String,
        headers: Map<String, String>,
        responseJson: String,
    ) {
        val tools =
            xTry("LLMController.handleMcpDiscoveryResponse:extract:$url") {
                extractDiscoveredTools(responseJson)
            } ?: return
        try {
            persistDiscoveredTools(url = url, headers = headers, tools = tools)
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) {
                throw throwable
            }
            xlog("LLMController.handleMcpDiscoveryResponse:persist failed for $url: ${throwable.message}")
        }
    }

    private suspend fun persistDiscoveredTools(
        url: String,
        headers: Map<String, String>,
        tools: List<McpCachedTool>,
    ) {
        val context = sessionContext ?: ContextProvider.await()
        mcpCacheWriteMutex.withLock {
            val latestSettings = XService.getLocalSettings(context)
            val latestCache =
                latestSettings.mcpDiscoveredToolsCache?.toMutableMap() ?: mutableMapOf()
            latestCache[mcpCacheKey(url = url, headers = headers)] = buildMcpCacheEntry(tools)

            val updatedProps = latestSettings.props.toMutableMap()
            updatedProps[MCP_DISCOVERED_TOOLS_CACHE_KEY] = JsonObject(latestCache)
            XService.putLocalSettings(context, LocalSettings(JsonObject(updatedProps)))
        }
    }

    private fun extractDiscoveredTools(responseJson: String): List<McpCachedTool> {
        val root = json.parseToJsonElement(responseJson).jsonObject
        val result = root["result"]?.jsonObject
            ?: error("MCP discovery response missing result object")
        return result["tools"]?.jsonArray.orEmpty().mapNotNull { element ->
            val tool = element as? JsonObject ?: return@mapNotNull null
            val name = tool["name"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val inputSchema = tool["inputSchema"] as? JsonObject ?: return@mapNotNull null
            if (name.isBlank()) {
                return@mapNotNull null
            }
            McpCachedTool(
                name = name,
                description = tool["description"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                inputSchema = inputSchema,
            )
        }
    }

    private fun buildMcpCacheEntry(tools: List<McpCachedTool>): JsonObject {
        return JsonObject(
            mapOf(
                "tools" to JsonArray(
                    tools.map { tool ->
                        JsonObject(
                            mapOf(
                                "name" to JsonPrimitive(tool.name),
                                "description" to JsonPrimitive(tool.description),
                                "inputSchema" to tool.inputSchema,
                            )
                        )
                    }
                ),
            )
        )
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