package com.niki914.nexus.agentic.mod.feat

import com.niki914.nexus.agentic.chat.ActiveTurnStore
import com.niki914.nexus.agentic.chat.ConversationTurnState
import com.niki914.nexus.agentic.chat.LLMController
import com.niki914.nexus.agentic.chat.TurnMode
import com.niki914.nexus.h.core.runtime.Hook
import com.niki914.nexus.h.xevent.XEvent
import com.niki914.nexus.h.xevent.XEventContext
import de.robv.android.xposed.callbacks.XC_LoadPackage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 抽象语音助手 Hook 基类，规范核心生命周期与功能职责
 *
 * TODO 统一所有业务下的 subhooks、云 config、AbstractAssistantHook 方法命名
 */
abstract class AbstractAssistantHook(protected val scope: CoroutineScope) : Hook {
    protected open val floatResumeGraceWindowMs: Long = 1500L

    protected fun installFloatScreenDetachHooks(
        lpparam: XC_LoadPackage.LoadPackageParam,
        detachTarget: HookTarget?,
        resumeTarget: HookTarget?
    ) {
        FloatScreenResetDetector(
            graceWindowMs = floatResumeGraceWindowMs,
            onReset = { scope.launch { onSessionReset() } }
        ).install(
            lpparam = lpparam,
            detachTarget = detachTarget,
            resumeTarget = resumeTarget
        )
    }

    final override fun onHook(lpparam: XC_LoadPackage.LoadPackageParam) {
        onBeforeInstallHooks(lpparam)
        installSessionHooks(lpparam)
        installResponseHooks(lpparam)
        installInputHooks(lpparam) { roomId, query ->
            scope.launch {
                handleCapturedQuery(roomId, query)
            }
        }
    }

    protected open fun onBeforeInstallHooks(lpparam: XC_LoadPackage.LoadPackageParam) = Unit

    private suspend fun handleCapturedQuery(roomId: String, query: String) {
        val nextTurnState = ConversationTurnState().nextTurn(
            query = query,
            mode = if (shouldTakeOver(query)) {
                TurnMode.NativeTakeover
            } else {
                TurnMode.InjectedLLM
            }
        )
        ActiveTurnStore.setCurrent(nextTurnState)
        val eventContext = XEventContext(
            roomId = roomId,
            turnId = nextTurnState.turnId,
            fields = mapOf("mode" to nextTurnState.mode.eventName())
        )
        XEvent.setContext(eventContext)
        XEvent.withContext(eventContext) {
            onTurnStateChanged(nextTurnState)
            XEvent.inputCaptured(
                fields = mapOf("queryLength" to query.length)
            )
            XEvent.turnDecided(
                fields = mapOf(
                    "queryLength" to query.length
                )
            )

            if (nextTurnState.mode == TurnMode.NativeTakeover) {
                LLMController.stopCurrentRound(keepCurrentTurn = false)
                return@withContext
            }

            dispatchQueryToLLM(
                turnId = nextTurnState.turnId,
                roomId = roomId,
                query = query
            )
        }
    }

    protected open suspend fun onTurnStateChanged(state: ConversationTurnState) = Unit

    protected open fun shouldTakeOver(query: String): Boolean = false

    private fun TurnMode.eventName(): String = when (this) {
        TurnMode.InjectedLLM -> "InjectedLLM"
        TurnMode.NativeTakeover -> "NativeTakeover"
    }

    protected open suspend fun onSessionReset() {
        LLMController.resetConversation()
        ActiveTurnStore.clear()
        XEvent.clearContext()
    }

    protected abstract fun installSessionHooks(lpparam: XC_LoadPackage.LoadPackageParam)

    protected abstract fun installResponseHooks(lpparam: XC_LoadPackage.LoadPackageParam)

    /**
     * 监听输入逻辑，当捕获到用户输入时，调用 [onInput] 回调
     */
    protected abstract fun installInputHooks(
        lpparam: XC_LoadPackage.LoadPackageParam,
        onInput: (roomId: String, query: String) -> Unit
    )

    /**
     * 将查询分发给 LLM SDK
     */
    protected abstract suspend fun dispatchQueryToLLM(turnId: Long, roomId: String, query: String)

    /**
     * 渲染流式返回的大模型文本卡片
     * @param turnId 当前轮次 ID
     * @param roomId 当前会话 roomId
     * @param chunk 累加后的流式文本块
     * @param isFirst 是否为第一片
     * @param isFinal 是否为最后一片
     */
    protected abstract suspend fun renderStreamCard(
        turnId: Long,
        roomId: String,
        chunk: String,
        isFirst: Boolean,
        isFinal: Boolean
    )
}
