package com.niki914.nexus.agentic.mod.feat.hyper.subhooks

import com.niki914.nexus.agentic.mod.feat.HookTarget
import com.niki914.nexus.agentic.mod.feat.SubHook
import com.niki914.nexus.agentic.mod.feat.hyper.XiaoaiConfigProvider
import com.niki914.nexus.h.util.xlog
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.callbacks.XC_LoadPackage

class CaptureInputHook(
    private val onInput: (dialogId: String, query: String) -> Unit
) : SubHook() {

    private val duplicateLock = Any()
    private var lastDeliveredInput: CapturedInput? = null

    private data class CapturedInput(
        val dialogId: String,
        val query: String
    )

    override val hookTarget: HookTarget?
        get() = XiaoaiConfigProvider.CaptureInput.hookTarget

    override fun beforeHook(param: XC_MethodHook.MethodHookParam) {
        val dialogIdArgIndex = XiaoaiConfigProvider.CaptureInput.dialogIdArgIndex ?: return
        val queryArgIndex = XiaoaiConfigProvider.CaptureInput.queryArgIndex ?: return

        val dialogId = param.args.getOrNull(dialogIdArgIndex) as? String
        val query = param.args.getOrNull(queryArgIndex) as? String

        if (dialogId.isNullOrBlank() || query.isNullOrBlank()) {
            xlog("[$name] 忽略无效输入: dialogId=$dialogId, query=$query")
            return
        }

        if (shouldSuppress(dialogId, query)) {
            xlog("[$name] 忽略重复输入: dialogId=$dialogId, query=$query")
            return
        }

        xlog("[$name] 捕获用户输入: $query (dialogId=$dialogId)")
        onInput(dialogId, query)
    }

    private fun shouldSuppress(dialogId: String, query: String): Boolean = synchronized(duplicateLock) {
        val currentInput = CapturedInput(dialogId = dialogId, query = query)
        if (lastDeliveredInput == currentInput) {
            true
        } else {
            lastDeliveredInput = currentInput
            false
        }
    }
}
