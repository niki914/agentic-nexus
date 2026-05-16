package com.niki914.nexus.agentic.mod.feat.hyper.subhooks

import com.niki914.nexus.agentic.mod.feat.HookTarget
import com.niki914.nexus.agentic.mod.feat.SubHook
import com.niki914.nexus.agentic.mod.feat.hyper.XiaoaiConfigProvider
import com.niki914.nexus.h.util.call
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.callbacks.XC_LoadPackage

/** 在宿主创建响应目标时捕获目标对象及其 dialogId，通过回调传给 XiaoaiChatHook 供后续注入。 */
class CaptureResponseTargetHook(
    private val onCaptured: (target: Any, dialogId: String) -> Unit = { _, _ -> }
) : SubHook() {

    override val hookTarget: HookTarget?
        get() = XiaoaiConfigProvider.CaptureResponseTarget.hookTarget

    override fun beforeHook(param: XC_MethodHook.MethodHookParam) {
        val instruction = param.args.firstOrNull() ?: return
        val dialogId = resolveDialogId(instruction, param.thisObject)
        if (dialogId.isNullOrBlank()) {
            return
        }
        onCaptured(param.thisObject, dialogId)
    }
}
