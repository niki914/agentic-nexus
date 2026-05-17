package com.niki914.nexus.agentic.mod.feat.oppo.subhooks

import com.niki914.nexus.agentic.mod.feat.HookTarget
import com.niki914.nexus.agentic.mod.feat.SubHook
import com.niki914.nexus.agentic.mod.feat.oppo.BreenoConfigProvider
import com.niki914.nexus.h.util.xlog
import de.robv.android.xposed.XC_MethodHook

/** 检测宿主创建新房间，触发 session 重置 */
class ResetSessionHook(
    private val onSessionReset: (roomId: String) -> Unit
) : SubHook() {

    override val hookTarget: HookTarget?
        get() = BreenoConfigProvider.ResetSession.hookTarget

    override fun afterHook(param: XC_MethodHook.MethodHookParam) {
        val roomId = param.result as? String ?: ""
        xlog("[$name] 新对话已创建! roomMode=${param.args.getOrNull(0)}, src=${param.args.getOrNull(1)}, roomId=$roomId")
        onSessionReset(roomId)
    }
}
