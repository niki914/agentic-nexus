package com.niki914.nexus.agentic.mod.feat.hyper.subhooks

import com.niki914.nexus.agentic.chat.ConversationTurnState
import com.niki914.nexus.agentic.chat.TurnMode
import com.niki914.nexus.agentic.mod.feat.hyper.XiaoaiConfigProvider
import com.niki914.nexus.h.core.runtime.Hook
import com.niki914.nexus.h.util.call
import com.niki914.nexus.h.util.hookMethod
import com.niki914.nexus.h.util.resolveParamTypes
import com.niki914.nexus.h.util.xlog
import de.robv.android.xposed.callbacks.XC_LoadPackage

class BlockNativeTtsPlaybackHook(
    private val resolveTurnState: (String?) -> ConversationTurnState?
) : Hook {
    override val name: String = "XiaoaiBlockNativeTtsPlaybackHook"

    override fun onHook(lpparam: XC_LoadPackage.LoadPackageParam) {
        val ownerClass = XiaoaiConfigProvider.blockNativeTtsPlaybackOwnerClass ?: return
        val methodName = XiaoaiConfigProvider.blockNativeTtsPlaybackMethodName ?: return
        val methodParams = XiaoaiConfigProvider.blockNativeTtsPlaybackMethodParams ?: return
        val hookTiming = XiaoaiConfigProvider.blockNativeTtsPlaybackHookTiming
        if (hookTiming != null && hookTiming != "before") {
            xlog("[$name] action[block_native_tts_playback] 暂不支持 hook_timing=$hookTiming，跳过安装")
            return
        }
        val paramTypes = resolveParamTypes(methodParams, lpparam) ?: return

        lpparam.hookMethod(
            className = ownerClass,
            methodName = methodName,
            *paramTypes,
            before = before@{ param ->
                val dialogIdGetter = XiaoaiConfigProvider.blockNativeTtsPlaybackTargetDialogIdGetter ?: return@before
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
        )
    }
}
