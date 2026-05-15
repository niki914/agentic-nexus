package com.niki914.nexus.agentic.mod.feat.hyper

import com.niki914.nexus.agentic.chat.LLMController
import com.niki914.nexus.agentic.chat.TurnMode
import com.niki914.nexus.agentic.mod.HookLocalSettings
import com.niki914.nexus.agentic.mod.feat.AbstractAssistantHook
import com.niki914.nexus.agentic.mod.feat.hyper.subhooks.BlockNativeTextStreamHook
import com.niki914.nexus.agentic.mod.feat.hyper.subhooks.BlockNativeTtsPlaybackHook
import com.niki914.nexus.agentic.mod.feat.hyper.subhooks.BlockNativeTtsStreamHook
import com.niki914.nexus.agentic.mod.feat.hyper.subhooks.CaptureInputHook
import com.niki914.nexus.agentic.mod.feat.hyper.subhooks.CaptureResponseTargetHook
import com.niki914.nexus.agentic.mod.feat.hyper.subhooks.RenderTextStreamCardHook
import com.niki914.nexus.h.util.hookMethod
import com.niki914.nexus.h.util.xlog
import de.robv.android.xposed.callbacks.XC_LoadPackage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn

class XiaoaiChatHook(
    scope: CoroutineScope
) : AbstractAssistantHook(scope) {
    override val name: String = "XiaoaiChatHook"

    private val responseTargetStore = ResponseTargetStore()
    private val injectedInstructionRegistry = InjectedInstructionRegistry()
    private var renderTextStreamCardHook: RenderTextStreamCardHook? = null

    private var targetReady = CompletableDeferred<Unit>()

    override suspend fun onSessionReset(roomId: String) {
        val previousDialogId = turnState.roomId
        super.onSessionReset(roomId)
        LLMController.resetConversation()
        targetReady.cancel()
        targetReady = CompletableDeferred()
        if (previousDialogId.isNotBlank()) {
            responseTargetStore.clear(previousDialogId)
        }
        renderTextStreamCardHook?.reset()
    }

    override fun installSessionHooks(lpparam: XC_LoadPackage.LoadPackageParam) {
        XiaoaiConfigProvider.floatWindowTargetActivityClass?.let {
            installTargetActivityResumeTracker(lpparam, it)
        }
        val floatClass = XiaoaiConfigProvider.floatWindowOwnerClass
        val detachMethod = XiaoaiConfigProvider.floatWindowDetachMethodName
        if (floatClass != null && detachMethod != null) {
            lpparam.hookMethod(
                className = floatClass,
                methodName = detachMethod,
                before = { onFloatDetach() }
            )
        }
    }

    override fun installResponseHooks(lpparam: XC_LoadPackage.LoadPackageParam) {
        CaptureResponseTargetHook(
            responseTargetStore = responseTargetStore,
            onCaptured = { targetReady.complete(Unit) }
        ).onHook(lpparam)

        BlockNativeTextStreamHook(
            injectedInstructionRegistry = injectedInstructionRegistry,
            resolveTurnState = { dialogId -> resolveTurnState(dialogId) }
        ).onHook(lpparam)

        BlockNativeTtsStreamHook(
            injectedInstructionRegistry = injectedInstructionRegistry,
            resolveTurnState = { dialogId -> resolveTurnState(dialogId) }
        ).onHook(lpparam)

        BlockNativeTtsPlaybackHook(
            resolveTurnState = { dialogId -> resolveTurnState(dialogId) }
        ).onHook(lpparam)

        renderTextStreamCardHook = RenderTextStreamCardHook(
            responseTargetStore = responseTargetStore,
            injectedInstructionRegistry = injectedInstructionRegistry
        ).also { it.onHook(lpparam) }
    }

    override fun installInputHooks(
        lpparam: XC_LoadPackage.LoadPackageParam,
        onInput: (roomId: String, query: String) -> Unit
    ) {
        CaptureInputHook(onInput = onInput).onHook(lpparam)
    }

    override fun shouldTakeOver(query: String): Boolean {
        return HookLocalSettings.current().takeoverKeywords.any { keyword ->
            keyword.isNotBlank() && query.contains(keyword)
        }
    }

    override suspend fun dispatchQueryToLLM(turnId: Long, roomId: String, query: String) {
        targetReady.cancel()
        targetReady = CompletableDeferred()

        val sharedFlow = LLMController.stream(query)
            .shareIn(scope, SharingStarted.Eagerly, replay = Int.MAX_VALUE)

        targetReady.await()

        sharedFlow.collect { (chunk, pos) ->
            val isFirst = pos == LLMController.Pos.First
            val isFinal = pos == LLMController.Pos.Final
            renderStreamCard(turnId, roomId, chunk, isFirst, isFinal)
        }
    }

    override suspend fun renderStreamCard(
        turnId: Long,
        roomId: String,
        chunk: String,
        isFirst: Boolean,
        isFinal: Boolean
    ) {
        if (!isActiveTurn(turnId, roomId)) {
            xlog(
                "[$name] 丢弃非当前注入轮次的文字流渲染: dialogId=$roomId, turnId=$turnId, activeTurn=${resolveTurnState(roomId)?.turnId}, mode=${resolveTurnState(roomId)?.mode}"
            )
            return
        }

        renderTextStreamCardHook?.render(
            turnId = turnId,
            dialogId = roomId,
            chunk = chunk,
            isFirst = isFirst,
            isFinal = isFinal
        )
    }

    private fun isActiveTurn(turnId: Long, roomId: String): Boolean {
        val activeTurn = resolveTurnState(roomId)
        return activeTurn?.turnId == turnId && activeTurn.mode == TurnMode.InjectedLLM
    }

    private fun resolveTurnState(dialogId: String?) =
        dialogId
            ?.takeIf { it.isNotBlank() }
            ?.takeIf { it == turnState.roomId }
            ?.let { turnState }
}
