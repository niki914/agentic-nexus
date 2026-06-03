package com.niki914.nexus.agentic.mod.feat.hyper.subhooks

import com.niki914.nexus.agentic.mod.feat.HookTarget
import com.niki914.nexus.agentic.mod.feat.SubHook
import com.niki914.nexus.agentic.mod.feat.hyper.XiaoaiConfigProvider
import de.robv.android.xposed.XC_MethodHook

/** 在宿主创建响应目标时捕获目标对象，供后续文字流分片注入。 */
class CaptureResponseTargetHook(
    private val onCaptured: (target: Any) -> Unit = {}
) : SubHook() {

    override val hookTarget: HookTarget?
        get() = XiaoaiConfigProvider.CaptureResponseTarget.hookTarget

    override fun beforeHook(param: XC_MethodHook.MethodHookParam) {
        val instruction = param.args.firstOrNull() ?: return
        val dialogId = resolveDialogId(instruction, param.thisObject)
        if (dialogId.isNullOrBlank()) {
            return
        }
        onCaptured(param.thisObject)
    }
}
