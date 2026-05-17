package com.niki914.nexus.agentic.mod.feat

import com.niki914.nexus.h.core.runtime.Hook
import com.niki914.nexus.h.util.hookMethod
import com.niki914.nexus.h.util.resolveParamTypes
import com.niki914.nexus.h.util.xlog
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.callbacks.XC_LoadPackage

abstract class SubHook : Hook {

    override val name: String = this::class.java.simpleName

    open val hookTarget: HookTarget? = null

    open fun beforeHook(param: XC_MethodHook.MethodHookParam) {}

    open fun afterHook(param: XC_MethodHook.MethodHookParam) {}

    override fun onHook(lpparam: XC_LoadPackage.LoadPackageParam) {
        val target = hookTarget
        if (target == null) {
            xlog("[SubHook] ${name}: hookTarget is null, skipping")
            return
        }

        val paramTypes = resolveParamTypes(target.methodParams, lpparam)
        if (paramTypes == null) {
            xlog("[SubHook] ${name}: failed to resolve paramTypes, skipping")
            return
        }

        when (val timing = target.hookTiming) {
            "after", "before" -> {
                xlog("[SubHook] ${name}: hookTiming=$timing, registering ${timing}-only hook")
                lpparam.hookMethod(
                    className = target.ownerClass,
                    methodName = target.methodName,
                    *paramTypes,
                    before = { param -> beforeHook(param) },
                    after = { param -> afterHook(param) }
                )
            }
            else -> Unit
        }
    }
}
