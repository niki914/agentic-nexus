package com.niki914.nexus.agentic.mod.feat.hyper.subhooks

import com.niki914.nexus.agentic.mod.feat.hyper.ResponseTargetStore
import com.niki914.nexus.agentic.mod.feat.hyper.XiaoaiConfigProvider
import com.niki914.nexus.h.core.runtime.Hook
import com.niki914.nexus.h.util.call
import com.niki914.nexus.h.util.hookMethod
import com.niki914.nexus.h.util.resolveParamTypes
import com.niki914.nexus.h.util.xlog
import de.robv.android.xposed.callbacks.XC_LoadPackage

class CaptureResponseTargetHook(
    private val responseTargetStore: ResponseTargetStore
) : Hook {
    override val name: String = "XiaoaiCaptureResponseTargetHook"

    override fun onHook(lpparam: XC_LoadPackage.LoadPackageParam) {
        val ownerClass = XiaoaiConfigProvider.captureResponseTargetOwnerClass ?: return
        val methodName = XiaoaiConfigProvider.captureResponseTargetMethodName ?: return
        val methodParams = XiaoaiConfigProvider.captureResponseTargetMethodParams ?: return
        val hookTiming = XiaoaiConfigProvider.captureResponseTargetHookTiming
        if (hookTiming != null && hookTiming != "before") {
            xlog("[$name] action[capture_response_target] 暂不支持 hook_timing=$hookTiming，跳过安装")
            return
        }
        val paramTypes = resolveParamTypes(methodParams, lpparam) ?: return

        lpparam.hookMethod(
            className = ownerClass,
            methodName = methodName,
            *paramTypes,
            before = before@{ param ->
                val instruction = param.args.firstOrNull() ?: return@before
                val dialogId = resolveDialogId(instruction, param.thisObject)
                if (dialogId.isNullOrBlank()) {
                    return@before
                }
                responseTargetStore.put(dialogId, param.thisObject)
            }
        )
    }

    private fun resolveDialogId(instruction: Any, target: Any?): String? {
        val dialogIdOptional = instruction.call<Any>(
            XiaoaiConfigProvider.captureResponseTargetInstructionDialogIdGetter
        )
        val dialogId = dialogIdOptional
            ?.takeIf {
                it.call<Boolean>(XiaoaiConfigProvider.captureResponseTargetOptionalHasValueMethod) == true
            }
            ?.call<String>(XiaoaiConfigProvider.captureResponseTargetOptionalValueGetter)
        if (!dialogId.isNullOrBlank()) {
            return dialogId
        }
        val targetDialogIdGetter = XiaoaiConfigProvider.captureResponseTargetTargetDialogIdGetter ?: return null
        return target?.call<String>(targetDialogIdGetter)
    }
}
