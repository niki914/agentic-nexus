package com.niki914.nexus.agentic.mod.feat.hyper.subhooks

import com.niki914.nexus.agentic.mod.feat.hyper.XiaoaiConfigProvider
import com.niki914.nexus.h.core.runtime.Hook
import com.niki914.nexus.h.util.hookMethod
import com.niki914.nexus.h.util.resolveParamTypes
import com.niki914.nexus.h.util.xlog
import de.robv.android.xposed.callbacks.XC_LoadPackage

class ResetSessionHook(
    private val onSessionReset: () -> Unit
) : Hook {
    override val name: String = "XiaoaiResetSessionHook"

    override fun onHook(lpparam: XC_LoadPackage.LoadPackageParam) {
        val ownerClass = XiaoaiConfigProvider.resetSessionOwnerClass ?: return
        val methodName = XiaoaiConfigProvider.resetSessionMethodName ?: return
        val methodParams = XiaoaiConfigProvider.resetSessionMethodParams ?: return
        val hookTiming = XiaoaiConfigProvider.resetSessionHookTiming ?: "after"
        val paramTypes = resolveParamTypes(methodParams, lpparam) ?: return

        val callback: (de.robv.android.xposed.XC_MethodHook.MethodHookParam) -> Unit = {
            xlog("[$name] 命中 reset_session: $ownerClass#$methodName")
            onSessionReset()
        }

        when (hookTiming) {
            "before" -> lpparam.hookMethod(
                className = ownerClass,
                methodName = methodName,
                *paramTypes,
                before = callback
            )

            "after" -> lpparam.hookMethod(
                className = ownerClass,
                methodName = methodName,
                *paramTypes,
                after = callback
            )

            else -> {
                xlog("[$name] action[reset_session] 暂不支持 hook_timing=$hookTiming，跳过安装")
                return
            }
        }
    }
}
