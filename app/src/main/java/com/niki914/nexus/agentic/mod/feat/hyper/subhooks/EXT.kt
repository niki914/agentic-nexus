package com.niki914.nexus.agentic.mod.feat.hyper.subhooks

import com.niki914.nexus.agentic.mod.feat.hyper.XiaoaiConfigProvider
import com.niki914.nexus.h.util.call

fun resolveDialogId(instruction: Any, target: Any?): String? {
    val dialogIdOptional = instruction.call<Any>(XiaoaiConfigProvider.BlockNativeTextStream.instructionDialogIdGetter ?: return null)
    val dialogId = dialogIdOptional
        ?.takeIf { it.call<Boolean>(XiaoaiConfigProvider.BlockNativeTextStream.optionalHasValueMethod ?: return null) == true }
        ?.call<String>(XiaoaiConfigProvider.BlockNativeTextStream.optionalValueGetter ?: return null)
    if (!dialogId.isNullOrBlank()) {
        return dialogId
    }
    val targetDialogIdGetter = XiaoaiConfigProvider.BlockNativeTextStream.targetDialogIdGetter ?: return null
    return target?.call<String>(targetDialogIdGetter)
}