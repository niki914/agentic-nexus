package com.niki914.nexus.agentic.mod.feat.hyper.subhooks

import com.niki914.nexus.agentic.chat.ConversationTurnState
import com.niki914.nexus.agentic.chat.TurnMode
import com.niki914.nexus.agentic.mod.feat.hyper.InjectedInstructionRegistry
import com.niki914.nexus.agentic.mod.feat.hyper.XiaoaiConfigProvider
import com.niki914.nexus.h.core.runtime.Hook
import com.niki914.nexus.h.util.call
import com.niki914.nexus.h.util.hookMethod
import com.niki914.nexus.h.util.resolveParamTypes
import com.niki914.nexus.h.util.xlog
import de.robv.android.xposed.callbacks.XC_LoadPackage

class BlockNativeTextStreamHook(
    private val injectedInstructionRegistry: InjectedInstructionRegistry,
    private val resolveTurnState: (String?) -> ConversationTurnState?
) : Hook {
    override val name: String = "XiaoaiBlockNativeTextStreamHook"

    override fun onHook(lpparam: XC_LoadPackage.LoadPackageParam) {
        val ownerClass = XiaoaiConfigProvider.blockNativeTextStreamOwnerClass ?: return
        val methodName = XiaoaiConfigProvider.blockNativeTextStreamMethodName ?: return
        val methodParams = XiaoaiConfigProvider.blockNativeTextStreamMethodParams ?: return
        val hookTiming = XiaoaiConfigProvider.blockNativeTextStreamHookTiming
        if (hookTiming != null && hookTiming != "before") {
            xlog("[$name] action[block_native_text_stream] 暂不支持 hook_timing=$hookTiming，跳过安装")
            return
        }
        val instructionFullName = XiaoaiConfigProvider.blockNativeTextStreamInstructionFullName ?: return
        val paramTypes = resolveParamTypes(methodParams, lpparam) ?: return

        lpparam.hookMethod(
            className = ownerClass,
            methodName = methodName,
            *paramTypes,
            before = before@{ param ->
                val instruction = param.args.firstOrNull() ?: return@before
                if (injectedInstructionRegistry.isInjected(instruction)) {
                    return@before
                }
                val fullName = instruction.call<String>(
                    XiaoaiConfigProvider.blockNativeTextStreamInstructionFullNameGetter
                )
                if (fullName != instructionFullName) {
                    return@before
                }
                val dialogId = resolveDialogId(instruction, param.thisObject)
                when (resolveTurnState(dialogId)?.mode) {
                    TurnMode.InjectedLLM -> {
                        xlog("[$name] 注入模式，拦截原生文字流: dialogId=$dialogId")
                        param.result = null
                    }

                    TurnMode.NativeTakeover -> {
                        xlog("[$name] takeover 模式，放行原生文字流: dialogId=$dialogId")
                    }

                    null -> Unit
                }
            }
        )
    }

    private fun resolveDialogId(instruction: Any, target: Any?): String? {
        val dialogIdOptional = instruction.call<Any>(
            XiaoaiConfigProvider.blockNativeTextStreamInstructionDialogIdGetter
        )
        val dialogId = dialogIdOptional
            ?.takeIf {
                it.call<Boolean>(XiaoaiConfigProvider.blockNativeTextStreamOptionalHasValueMethod) == true
            }
            ?.call<String>(XiaoaiConfigProvider.blockNativeTextStreamOptionalValueGetter)
        if (!dialogId.isNullOrBlank()) {
            return dialogId
        }
        val targetDialogIdGetter = XiaoaiConfigProvider.blockNativeTextStreamTargetDialogIdGetter ?: return null
        return target?.call<String>(targetDialogIdGetter)
    }
}
