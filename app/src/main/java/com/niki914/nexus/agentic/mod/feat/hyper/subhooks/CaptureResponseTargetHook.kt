package com.niki914.nexus.agentic.mod.feat.hyper.subhooks

import com.niki914.nexus.agentic.mod.feat.HookTarget
import com.niki914.nexus.agentic.mod.feat.SubHook
import com.niki914.nexus.agentic.mod.feat.hyper.ResponseTargetStore
import com.niki914.nexus.agentic.mod.feat.hyper.XiaoaiConfigProvider
import com.niki914.nexus.h.util.call
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.callbacks.XC_LoadPackage

class CaptureResponseTargetHook(
    private val responseTargetStore: ResponseTargetStore,
    private val onCaptured: () -> Unit = {}
) : SubHook() {

    override val hookTarget: HookTarget?
        get() = XiaoaiConfigProvider.CaptureResponseTarget.hookTarget

    override fun beforeHook(param: XC_MethodHook.MethodHookParam) {
        val instruction = param.args.firstOrNull() ?: return
        val dialogId = resolveDialogId(instruction, param.thisObject)
        if (dialogId.isNullOrBlank()) {
            return
        }
        responseTargetStore.put(dialogId, param.thisObject)
        onCaptured()
    }

    private fun resolveDialogId(instruction: Any, target: Any?): String? {
        val dialogIdOptional = instruction.call<Any>(
            XiaoaiConfigProvider.CaptureResponseTarget.instructionDialogIdGetter
        )
        val dialogId = dialogIdOptional
            ?.takeIf {
                it.call<Boolean>(XiaoaiConfigProvider.CaptureResponseTarget.optionalHasValueMethod) == true
            }
            ?.call<String>(XiaoaiConfigProvider.CaptureResponseTarget.optionalValueGetter)
        if (!dialogId.isNullOrBlank()) {
            return dialogId
        }
        val targetDialogIdGetter = XiaoaiConfigProvider.CaptureResponseTarget.targetDialogIdGetter ?: return null
        return target?.call<String>(targetDialogIdGetter)
    }
}
