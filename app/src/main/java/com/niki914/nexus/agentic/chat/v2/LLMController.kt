package com.niki914.nexus.agentic.chat.v2

import android.content.Context
import com.niki914.nexus.agentic.mod.HookLocalSettings
import com.niki914.nexus.h.util.ContextProvider
import com.niki914.s3ss10n.Session
import com.niki914.s3ss10n.SessionProtocols
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
        val session = obtainSession()
        val config = ResolvedLlmConfig(
            endpoint = settings.endpoint,
            apiKey = settings.apiKey,
            model = settings.model,
            baseSystemPrompt = settings.prompt,
            finalSystemPrompt = prompt.finalSystemPrompt,
            proxy = settings.proxy,
        )
        session.update {
            endpoint = config.endpoint
            apiKey = config.apiKey
            model = config.model
            systemPrompt = config.finalSystemPrompt

            // TODO 注入代理等额外网络设置。
            // TODO 注册 builtin/custom local tools。
            // TODO 注册 MCP server。
            // TODO 安装 tool hooks，并将 SessionEvent.Tool* 转换到 v2 事件流。
        }
        return LlmRuntimeSnapshot(
            settings = settings,
            config = config,
            tools = tools,
            prompt = prompt,
        ).also { snapshot ->
            runtimeState = RuntimeState(snapshot = snapshot)
        }
    }

    suspend fun refreshFromHookContext(): LlmRuntimeSnapshot {
        val context = ContextProvider.await()
        return refresh(context)
    }

    suspend fun snapshot(): LlmRuntimeSnapshot? = runtimeState?.snapshot

    fun stream(query: String): Flow<LlmStreamEvent> = callbackFlow {
        val state = refreshIfPossibleFromHookContext() ?: runtimeState
        if (state == null) {
            trySend(LlmStreamEvent.Error("LLM runtime is not ready"))
            awaitClose {}
            return@callbackFlow
        }

        trySend(
            LlmStreamEvent.Error(
                message = "TODO: wire Session.send(query) to v2 event stream"
            )
        )
        awaitClose {}
    }.flowOn(Dispatchers.IO)

    suspend fun resetConversation() {
        session?.resetConversation()
    }

    private suspend fun refreshIfPossibleFromHookContext(): RuntimeState? {
        return runCatching {
            refreshFromHookContext()
            runtimeState
        }.getOrNull()
    }

    private suspend fun obtainSession(): Session {
        return session ?: openSession().also { session = it }
    }

    private suspend fun openSession(): Session {
        return Session.open<SessionProtocols.OpenAI> {
            // TODO 初始 Session DSL 注册统一放在这里，避免散落在 refresh/stream 里。
        }
    }

    private fun buildMemorySections(settings: com.niki914.nexus.agentic.mod.LocalSettings): List<String> {
        // TODO 通过 XService / LocalSettings 读取记忆，并转成 prompt section。
        return emptyList()
    }

    private fun buildRuntimeSections(settings: com.niki914.nexus.agentic.mod.LocalSettings): List<String> {
        // TODO 注入宿主品牌、运行环境、能力边界等运行时上下文。
        return emptyList()
    }

    private data class RuntimeState(
        val snapshot: LlmRuntimeSnapshot,
    )
}
