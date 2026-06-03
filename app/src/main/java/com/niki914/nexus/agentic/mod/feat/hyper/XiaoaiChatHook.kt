package com.niki914.nexus.agentic.mod.feat.hyper

import com.niki914.nexus.agentic.chat.ActiveTurnStore
import com.niki914.nexus.agentic.chat.LLMController
import com.niki914.nexus.agentic.chat.collectAsChunk
import com.niki914.nexus.agentic.mod.HookLocalSettings
import com.niki914.nexus.agentic.mod.feat.AbstractAssistantHook
import com.niki914.nexus.agentic.mod.feat.hyper.subhooks.BlockNativeInstructionByWhitelistHook
import com.niki914.nexus.agentic.mod.feat.hyper.subhooks.BlockNativeTtsPlaybackHook
import com.niki914.nexus.agentic.mod.feat.hyper.subhooks.CaptureInputHook
import com.niki914.nexus.agentic.mod.feat.hyper.subhooks.CaptureResponseTargetHook
import com.niki914.nexus.agentic.mod.feat.hyper.subhooks.RenderTextStreamCardHook
import com.niki914.nexus.h.util.xlog
import de.robv.android.xposed.callbacks.XC_LoadPackage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn

/** XiaoAi 宿主主 Hook，编排全部子 Hook 安装、会话生命周期、关键词接管判定及 LLM 流式分片注入管线。 */
// TODO P0 提示小爱为 Beta，先发版
class XiaoaiChatHook( // TODO P0 NewRoom / 卡片采用白名单模式避免放行不正确的卡片
    scope: CoroutineScope
) : AbstractAssistantHook(scope) {
    override val name: String = "XiaoaiChatHook"

    private var renderTextStreamCardHook: RenderTextStreamCardHook? = null

    @Volatile
    private var capturedResponseTarget: Any? = null
    private var targetReady = CompletableDeferred<Unit>()

    override suspend fun onSessionReset() {
        super.onSessionReset()
        LLMController.resetConversation()
        targetReady.cancel()
        targetReady = CompletableDeferred()
        capturedResponseTarget = null
        renderTextStreamCardHook?.reset()
    }

    override fun installSessionHooks(lpparam: XC_LoadPackage.LoadPackageParam) {
        installFloatScreenDetachHooks(
            lpparam = lpparam,
            detachTarget = XiaoaiConfigProvider.FloatScreenDetach.detachTarget,
            resumeTarget = XiaoaiConfigProvider.FloatScreenDetach.resumeTarget
        )
    }

    override fun installResponseHooks(lpparam: XC_LoadPackage.LoadPackageParam) {
        CaptureResponseTargetHook(
            onCaptured = { target ->
                capturedResponseTarget = target
                targetReady.complete(Unit)
            }
        ).onHook(lpparam)

        BlockNativeInstructionByWhitelistHook().onHook(lpparam)

        BlockNativeTtsPlaybackHook().onHook(lpparam)

        renderTextStreamCardHook = RenderTextStreamCardHook()
            .also { it.onHook(lpparam) }
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

        targetReady.await() // TODO P1 死等风险

        sharedFlow.collectAsChunk { frame ->
            renderStreamCard(turnId, roomId, frame.text, frame.isFirst, frame.isFinal)
        }
    }

    override suspend fun renderStreamCard(
        turnId: Long,
        roomId: String,
        chunk: String,
        isFirst: Boolean,
        isFinal: Boolean
    ) {
        if (!ActiveTurnStore.isActiveInjection(turnId)) {
            val activeTurn = ActiveTurnStore.getCurrent()
            xlog(
                "[$name] 丢弃非当前注入轮次的文字流渲染: dialogId=$roomId, turnId=$turnId, activeTurn=${activeTurn?.turnId}, mode=${activeTurn?.mode}"
            )
            return
        }

        renderTextStreamCardHook?.render(
            turnId = turnId,
            dialogId = roomId,
            target = capturedResponseTarget,
            chunk = chunk,
            isFirst = isFirst,
            isFinal = isFinal
        )
    }
}
