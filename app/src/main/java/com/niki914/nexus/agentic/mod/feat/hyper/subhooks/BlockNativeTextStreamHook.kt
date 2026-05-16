package com.niki914.nexus.agentic.mod.feat.hyper.subhooks

import com.niki914.nexus.agentic.chat.ConversationTurnState
import com.niki914.nexus.agentic.chat.TurnMode
import com.niki914.nexus.agentic.mod.feat.HookTarget
import com.niki914.nexus.agentic.mod.feat.SubHook
import com.niki914.nexus.agentic.mod.feat.hyper.InjectedInstructionRegistry
import com.niki914.nexus.agentic.mod.feat.hyper.XiaoaiConfigProvider
import com.niki914.nexus.h.util.call
import com.niki914.nexus.h.util.xlog
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.callbacks.XC_LoadPackage

class BlockNativeTextStreamHook(
    private val injectedInstructionRegistry: InjectedInstructionRegistry,
    private val resolveTurnState: (String?) -> ConversationTurnState?
) : SubHook() {

    override val hookTarget: HookTarget?
        get() = XiaoaiConfigProvider.BlockNativeTextStream.hookTarget

    override fun beforeHook(param: XC_MethodHook.MethodHookParam) {
        val instruction = param.args.firstOrNull() ?: return
        if (injectedInstructionRegistry.isInjected(instruction)) {
            return
        }

        val fullName = instruction.call<String>(XiaoaiConfigProvider.BlockNativeTextStream.instructionFullNameGetter ?: return)
        val instructionFullName = XiaoaiConfigProvider.BlockNativeTextStream.instructionFullName ?: return
        if (fullName != instructionFullName) {
            return
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
}
