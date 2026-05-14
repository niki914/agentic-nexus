package com.niki914.nexus.agentic.mod.feat.hyper.subhooks

import com.niki914.nexus.agentic.mod.feat.hyper.XiaoaiConfigProvider
import com.niki914.nexus.h.core.runtime.Hook
import com.niki914.nexus.h.util.hookMethod
import com.niki914.nexus.h.util.resolveParamTypes
import com.niki914.nexus.h.util.xlog
import de.robv.android.xposed.callbacks.XC_LoadPackage

class CaptureInputHook(
    private val onInput: (dialogId: String, query: String) -> Unit
) : Hook {
    override val name: String = "XiaoaiCaptureInputHook"

    private val duplicateLock = Any()
    private var lastDeliveredInput: CapturedInput? = null

    private data class CapturedInput(
        val dialogId: String,
        val query: String
    )

    override fun onHook(lpparam: XC_LoadPackage.LoadPackageParam) {
        val operationManagerClass = XiaoaiConfigProvider.captureInputOwnerClass ?: return
        val setQueryInfoMethodName = XiaoaiConfigProvider.captureInputMethodName ?: return
        val setQueryInfoMethodParams = XiaoaiConfigProvider.captureInputMethodParams ?: return
        val dialogIdArgIndex = XiaoaiConfigProvider.captureInputDialogIdArgIndex ?: return
        val queryArgIndex = XiaoaiConfigProvider.captureInputQueryArgIndex ?: return
        val hookTiming = XiaoaiConfigProvider.captureInputHookTiming
        if (hookTiming != null && hookTiming != "before") {
            xlog("[$name] action[capture_input] 暂不支持 hook_timing=$hookTiming，跳过安装")
            return
        }
        val paramTypes = resolveParamTypes(setQueryInfoMethodParams, lpparam) ?: return

        lpparam.hookMethod(
            className = operationManagerClass,
            methodName = setQueryInfoMethodName,
            *paramTypes,
            before = before@{ param ->
                val dialogId = param.args.getOrNull(dialogIdArgIndex) as? String
                val query = param.args.getOrNull(queryArgIndex) as? String

                if (dialogId.isNullOrBlank() || query.isNullOrBlank()) {
                    xlog("[$name] 忽略无效输入: dialogId=$dialogId, query=$query")
                    return@before
                }

                if (shouldSuppress(dialogId, query)) {
                    xlog("[$name] 忽略重复输入: dialogId=$dialogId, query=$query")
                    return@before
                }

                xlog("[$name] 捕获用户输入: $query (dialogId=$dialogId)")
                onInput(dialogId, query)
            }
        )
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
