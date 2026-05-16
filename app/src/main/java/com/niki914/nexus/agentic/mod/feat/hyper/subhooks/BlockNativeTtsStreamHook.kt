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

/** 在 InjectedLLM 模式下拦截原生 TTS 文本流指令，避免注入回复被原生语音播报覆盖。 */
class BlockNativeTtsStreamHook(
    private val injectedInstructionRegistry: InjectedInstructionRegistry,
    private val resolveTurnState: (String?) -> ConversationTurnState?
) : SubHook() {

    override val hookTarget: HookTarget?
        get() = XiaoaiConfigProvider.BlockNativeTtsStream.hookTarget

    override fun beforeHook(param: XC_MethodHook.MethodHookParam) {
        val instruction = param.args.firstOrNull() ?: return
        if (injectedInstructionRegistry.isInjected(instruction)) {
            return
        }
        val fullName = instruction.call<String>(XiaoaiConfigProvider.BlockNativeTtsStream.instructionFullNameGetter ?: return)
        val instructionFullName = XiaoaiConfigProvider.BlockNativeTtsStream.instructionFullName ?: return
        if (fullName != instructionFullName) {
            return
        }
        val dialogId = resolveDialogId(instruction, param.thisObject)
        when (resolveTurnState(dialogId)?.mode) {
            TurnMode.InjectedLLM -> {
                xlog("[$name] 注入模式，拦截原生 TTS 文本流: dialogId=$dialogId")
                param.result = null
            }

            TurnMode.NativeTakeover -> {
                xlog("[$name] takeover 模式，放行原生 TTS 文本流: dialogId=$dialogId")
            }

            null -> Unit
        }
    }
}
