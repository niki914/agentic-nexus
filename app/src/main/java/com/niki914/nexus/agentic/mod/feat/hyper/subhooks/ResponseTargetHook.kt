package com.niki914.nexus.agentic.mod.feat.hyper.subhooks

import com.niki914.nexus.agentic.mod.feat.hyper.XiaoaiConfigProvider
import com.niki914.nexus.h.core.runtime.Hook
import com.niki914.nexus.h.util.call
import com.niki914.nexus.h.util.hookMethod
import com.niki914.nexus.h.util.resolveParamTypes
import com.niki914.nexus.h.util.xlog
import de.robv.android.xposed.callbacks.XC_LoadPackage

class ResponseTargetHook(
    private val onTargetObserved: (card: Any, dialogId: String) -> Unit
) : Hook {
    override val name: String = "XiaoaiResponseTargetHook"

    override fun onHook(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (!XiaoaiConfigProvider.captureResponseTargetEnabled) {
            xlog("[$name] action[capture_response_target] 已禁用，跳过安装")
            return
        }

        val ownerClass = XiaoaiConfigProvider.captureResponseTargetOwnerClass ?: return
        val methodName = XiaoaiConfigProvider.captureResponseTargetMethodName ?: return
        val methodParams = XiaoaiConfigProvider.captureResponseTargetMethodParams ?: return
        val dialogIdGetter = XiaoaiConfigProvider.captureResponseTargetDialogIdGetter ?: return
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
                val card = param.thisObject ?: return@before
                val dialogId = card.call<String>(dialogIdGetter)
                if (dialogId.isNullOrBlank()) {
                    xlog("[$name] 忽略缺失 dialogId 的响应卡实例")
                    return@before
                }

                xlog("[$name] 捕获响应目标实例: dialogId=$dialogId, card=${card.javaClass.name}")
                onTargetObserved(card, dialogId)
            }
        )
    }
}
