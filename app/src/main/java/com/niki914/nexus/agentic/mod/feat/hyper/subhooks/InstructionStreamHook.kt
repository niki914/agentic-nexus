package com.niki914.nexus.agentic.mod.feat.hyper.subhooks

import com.niki914.nexus.h.core.runtime.Hook
import com.niki914.nexus.h.util.call
import com.niki914.nexus.h.util.hookMethod
import com.niki914.nexus.h.util.resolveClass
import com.niki914.nexus.h.util.xlog
import de.robv.android.xposed.callbacks.XC_LoadPackage

class InstructionStreamHook(
    private val onOperationObserved: (
        operation: Any,
        dialogId: String,
        instruction: Any?,
        instructionFullName: String?
    ) -> Unit,
    private val shouldBlock: (
        dialogId: String,
        instruction: Any?,
        instructionFullName: String?
    ) -> Boolean
) : Hook {
    override val name: String = "XiaoaiInstructionStreamHook"

    override fun onHook(lpparam: XC_LoadPackage.LoadPackageParam) {
        val instructionClass = resolveClass("com.xiaomi.ai.api.common.Instruction", lpparam) ?: return

        lpparam.hookMethod(
            className = "cb0.eb",
            methodName = "A0",
            instructionClass,
            before = before@{ param ->
                val operation = param.thisObject ?: return@before
                val dialogId = operation.call<String>("getDialogId")
                val instruction = param.args.firstOrNull()
                val instructionFullName = instruction?.call<String>("getFullName")

                if (dialogId.isNullOrBlank()) {
                    xlog("[$name] 忽略缺失 dialogId 的流式 Operation")
                    return@before
                }

                onOperationObserved(operation, dialogId, instruction, instructionFullName)

                if (!shouldBlock(dialogId, instruction, instructionFullName)) {
                    return@before
                }

                xlog("[$name] 拦截原生流指令: dialogId=$dialogId, fullName=$instructionFullName")
                param.result = null
            }
        )
    }
}
