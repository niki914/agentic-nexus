package com.niki914.nexus.agentic.mod.feat.hyper.subhooks

import com.niki914.nexus.agentic.chat.ConversationTurnState
import com.niki914.nexus.agentic.chat.TurnMode
import com.niki914.nexus.agentic.mod.feat.HookTarget
import com.niki914.nexus.agentic.mod.feat.SubHook
import com.niki914.nexus.agentic.mod.feat.hyper.XiaoaiConfigProvider
import com.niki914.nexus.h.util.call
import com.niki914.nexus.h.util.getTag
import com.niki914.nexus.h.util.xlog
import de.robv.android.xposed.XC_MethodHook

/** 在 InjectedLLM 模式下按白名单放行必要原生 Instruction，其余原生样式默认拦截。 */
class BlockNativeInstructionByWhitelistHook(
    private val resolveTurnState: (String?) -> ConversationTurnState?
) : SubHook() {

    override val hookTarget: HookTarget?
        get() = XiaoaiConfigProvider.BlockNativeInstructionWhitelist.hookTarget

    override fun beforeHook(param: XC_MethodHook.MethodHookParam) {
        val instruction = param.args.firstOrNull() ?: return
        if (instruction.getTag<Boolean>(INJECTED_FLAG) == true) {
            return
        }

        val config = XiaoaiConfigProvider.BlockNativeInstructionWhitelist
        val dialogId = resolveDialogId(
            instruction = instruction,
            target = param.thisObject,
            instructionDialogIdGetter = config.instructionDialogIdGetter,
            optionalHasValueMethod = config.optionalHasValueMethod,
            optionalValueGetter = config.optionalValueGetter,
            targetDialogIdGetter = config.targetDialogIdGetter,
        )
        when (resolveTurnState(dialogId)?.mode) {
            TurnMode.InjectedLLM -> Unit
            TurnMode.NativeTakeover -> {
                xlog("[$name] takeover 模式，放行原生 Instruction: dialogId=$dialogId")
                return
            }

            null -> return
        }

        val fullName = instruction.call<String>(config.instructionFullNameGetter)
        val allowedFullNames = config.allowedInstructionFullNames
        if (fullName != null && fullName in allowedFullNames) {
            xlog("[$name] 注入模式，白名单放行原生 Instruction: fullName=$fullName, dialogId=$dialogId")
            return
        }

        xlog("[$name] 注入模式，拦截非白名单原生 Instruction: fullName=$fullName, dialogId=$dialogId")
        param.result = null
    }
}
