package com.niki914.nexus.agentic.mod.feat.hyper.subhooks

import com.niki914.nexus.agentic.mod.feat.hyper.XiaoaiConfigProvider
import com.niki914.nexus.h.util.call

const val INJECTED_FLAG = "__nexus_injected" // TODO 上云

fun resolveDialogId(instruction: Any, target: Any?): String? {
    val dialogIdOptional = instruction.call<Any>(XiaoaiConfigProvider.BlockNativeTextStream.instructionDialogIdGetter)
    val dialogId = dialogIdOptional
        ?.takeIf { it.call<Boolean>(XiaoaiConfigProvider.BlockNativeTextStream.optionalHasValueMethod) == true }
        ?.call<String>(XiaoaiConfigProvider.BlockNativeTextStream.optionalValueGetter)
    if (!dialogId.isNullOrBlank()) {
        return dialogId
    }
    val targetDialogIdGetter = XiaoaiConfigProvider.BlockNativeTextStream.targetDialogIdGetter
    return target?.call<String>(targetDialogIdGetter)
}