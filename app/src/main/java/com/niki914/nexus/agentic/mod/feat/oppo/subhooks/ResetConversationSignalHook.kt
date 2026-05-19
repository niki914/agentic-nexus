package com.niki914.nexus.agentic.mod.feat.oppo.subhooks

import com.niki914.nexus.agentic.mod.feat.HookTarget
import com.niki914.nexus.agentic.mod.feat.SubHook
import com.niki914.nexus.agentic.mod.feat.oppo.BreenoConfigProvider
import com.niki914.nexus.h.util.xlog
import de.robv.android.xposed.XC_MethodHook

/** 检测宿主显式发出的会话重置信号 */
class ResetConversationSignalHook(
    private val onSessionReset: (roomId: String) -> Unit
) : SubHook() {

    override val hookTarget: HookTarget?
        get() = BreenoConfigProvider.ResetConversationSignal.hookTarget

    override fun afterHook(param: XC_MethodHook.MethodHookParam) {
        val roomId = param.result as? String ?: ""
        xlog("[$name] 检测到会话重置信号, roomMode=${param.args.getOrNull(0)}, src=${param.args.getOrNull(1)}, roomId=$roomId")
        onSessionReset(roomId)
    }
}
