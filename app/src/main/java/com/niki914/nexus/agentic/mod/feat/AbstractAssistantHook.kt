package com.niki914.nexus.agentic.mod.feat

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.niki914.nexus.agentic.chat.ConversationJournal
import com.niki914.nexus.agentic.chat.ConversationTurnState
import com.niki914.nexus.agentic.chat.TurnMode
import com.niki914.nexus.h.core.runtime.Hook
import com.niki914.nexus.h.util.hookMethod
import com.niki914.nexus.h.util.resolveParamTypes
import de.robv.android.xposed.callbacks.XC_LoadPackage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 抽象语音助手 Hook 基类，规范核心生命周期与功能职责
 *
 * TODO 统一所有业务下的 subhooks、云 config、AbstractAssistantHook 方法命名
 */
abstract class AbstractAssistantHook(protected val scope: CoroutineScope) : Hook {
    protected var turnState: ConversationTurnState = ConversationTurnState()

    protected open val floatResumeGraceWindowMs: Long = 700L

    private var lastFloatDetachObservedElapsed: Long = 0
    private var lastFloatResumeObservedElapsed: Long = 0
    private val floatResetHandler = Handler(Looper.getMainLooper())
    private var pendingFloatResetCheck: Runnable? = null

    protected fun installFloatScreenDetachHooks(
        lpparam: XC_LoadPackage.LoadPackageParam,
        detachTarget: HookTarget?,
        resumeTarget: HookTarget?
    ) {
        if (detachTarget == null || resumeTarget == null) return

        installHookTargetObserver(
            lpparam = lpparam,
            target = detachTarget,
            onObserved = ::onFloatScreenDetachObserved
        )
        installHookTargetObserver(
            lpparam = lpparam,
            target = resumeTarget,
            onObserved = ::onFloatResumeObserved
        )
    }

    protected fun onFloatScreenDetachObserved() {
        val detachObservedElapsed = SystemClock.elapsedRealtime()
        lastFloatDetachObservedElapsed = detachObservedElapsed
        pendingFloatResetCheck?.let { floatResetHandler.removeCallbacks(it) }

        val check = Runnable {
            val resumeObservedElapsed = lastFloatResumeObservedElapsed
            val hasResumedAfterDetach = resumeObservedElapsed >= detachObservedElapsed
            val resumeElapsedSinceDetach = resumeObservedElapsed - detachObservedElapsed
            if (!hasResumedAfterDetach || resumeElapsedSinceDetach > floatResumeGraceWindowMs) {
                scope.launch { onSessionReset("") }
            }
        }
        pendingFloatResetCheck = check
        floatResetHandler.postDelayed(check, floatResumeGraceWindowMs)
    }

    protected fun onFloatResumeObserved() {
        lastFloatResumeObservedElapsed = SystemClock.elapsedRealtime()
    }

    private fun installHookTargetObserver(
        lpparam: XC_LoadPackage.LoadPackageParam,
        target: HookTarget,
        onObserved: () -> Unit
    ) {
        val paramTypes = resolveParamTypes(target.methodParams, lpparam) ?: return
        registerBeforeOrAfterObserver(
            lpparam = lpparam,
            target = target,
            paramTypes = paramTypes,
            onObserved = onObserved
        )
    }

    private fun registerBeforeOrAfterObserver(
        lpparam: XC_LoadPackage.LoadPackageParam,
        target: HookTarget,
        paramTypes: Array<Class<*>>,
        onObserved: () -> Unit
    ) {
        when (target.hookTiming?.lowercase()) {
            "before" -> lpparam.hookMethod(
                className = target.ownerClass,
                methodName = target.methodName,
                *paramTypes,
                before = { onObserved() }
            )
            "after" -> lpparam.hookMethod(
                className = target.ownerClass,
                methodName = target.methodName,
                *paramTypes,
                after = { onObserved() }
            )
            else -> Unit
        }
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
        val nextTurnState = turnState.nextTurn(
            roomId = roomId,
            query = query,
            mode = if (shouldTakeOver(query)) {
                TurnMode.NativeTakeover
            } else {
                TurnMode.InjectedLLM
            }
        )
        turnState = nextTurnState
        onTurnStateChanged(nextTurnState)

        if (nextTurnState.mode == TurnMode.NativeTakeover) {
            onTakeoverTriggered(
                turnId = nextTurnState.turnId,
                roomId = roomId,
                query = query
            )
            return
        }

        dispatchQueryToLLM(
            turnId = nextTurnState.turnId,
            roomId = roomId,
            query = query
        )
    }

    protected open suspend fun onTurnStateChanged(state: ConversationTurnState) = Unit

    protected open fun shouldTakeOver(query: String): Boolean = false

    protected open suspend fun onTakeoverTriggered(turnId: Long, roomId: String, query: String) = Unit

    protected open suspend fun onSessionReset(roomId: String = "") {
        if (turnState.roomId.isNotBlank()) {
            ConversationJournal.clearRoom(turnState.roomId)
        }
        turnState = turnState.resetForRoom(roomId)
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
