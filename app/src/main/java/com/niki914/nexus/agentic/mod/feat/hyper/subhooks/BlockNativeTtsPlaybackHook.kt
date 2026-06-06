package com.niki914.nexus.agentic.mod.feat.hyper.subhooks

import com.niki914.nexus.agentic.chat.ActiveTurnStore
import com.niki914.nexus.agentic.chat.TurnMode
import com.niki914.nexus.agentic.mod.feat.HookTarget
import com.niki914.nexus.agentic.mod.feat.SubHook
import com.niki914.nexus.agentic.mod.feat.hyper.XiaoaiConfigProvider
import com.niki914.nexus.h.util.xlog
import com.niki914.nexus.h.xevent.XEvent
import de.robv.android.xposed.XC_MethodHook
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** 在 InjectedLLM 模式下拦截原生 TTS 播放调用，阻止注入回复期间的原生语音播报。 */
class BlockNativeTtsPlaybackHook(
    private val scope: CoroutineScope
) : SubHook() {

    override val hookTarget: HookTarget?
        get() = XiaoaiConfigProvider.BlockNativeTtsPlayback.hookTarget

    override fun beforeHook(param: XC_MethodHook.MethodHookParam) {
        val activeTurn = ActiveTurnStore.getCurrent()
        when (activeTurn?.mode) {
            TurnMode.InjectedLLM -> {
                xlog("[$name] 注入模式，拦截原生 TTS 播放")
                param.result = true
                val eventContext = XEvent.snapshotContext()
                scope.launch {
                    XEvent.withContext(eventContext) {
                        XEvent.nativeResponseBlocked(
                            fields = mapOf(
                                "host" to "xiaoai",
                                "source" to name,
                                "kind" to "tts",
                                "reason" to "tts_blocked"
                            )
                        )
                    }
                }
            }

            TurnMode.NativeTakeover -> {
                xlog("[$name] takeover 模式，放行原生 TTS 播放")
            }

            null -> Unit
        }
    }
}
