package com.niki914.nexus.agentic.mod.feat.hyper.subhooks

import com.niki914.nexus.agentic.chat.ConversationTurnState
import com.niki914.nexus.agentic.chat.TurnMode
import com.niki914.nexus.agentic.mod.feat.HookTarget
import com.niki914.nexus.agentic.mod.feat.SubHook
import com.niki914.nexus.agentic.mod.feat.hyper.XiaoaiConfigProvider
import com.niki914.nexus.h.util.call
import com.niki914.nexus.h.util.xlog
import de.robv.android.xposed.XC_MethodHook

/** 在 InjectedLLM 模式下拦截原生 TTS 播放调用，阻止注入回复期间的原生语音播报。 */
class BlockNativeTtsPlaybackHook(
    private val resolveTurnState: (String?) -> ConversationTurnState?
) : SubHook() {

    override val hookTarget: HookTarget?
        get() = XiaoaiConfigProvider.BlockNativeTtsPlayback.hookTarget

    override fun beforeHook(param: XC_MethodHook.MethodHookParam) {
        val dialogIdGetter = XiaoaiConfigProvider.BlockNativeTtsPlayback.targetDialogIdGetter ?: return
        val dialogId = param.thisObject.call<String>(dialogIdGetter)
        when (resolveTurnState(dialogId)?.mode) {
            TurnMode.InjectedLLM -> {
                xlog("[$name] 注入模式，拦截原生 TTS 播放: dialogId=$dialogId")
                param.result = true
            }

            TurnMode.NativeTakeover -> {
                xlog("[$name] takeover 模式，放行原生 TTS 播放: dialogId=$dialogId")
            }

            null -> Unit
        }
    }
}
