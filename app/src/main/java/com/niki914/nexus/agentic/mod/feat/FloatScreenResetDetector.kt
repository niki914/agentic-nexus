package com.niki914.nexus.agentic.mod.feat

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.niki914.nexus.h.util.hookMethod
import com.niki914.nexus.h.util.resolveParamTypes
import de.robv.android.xposed.callbacks.XC_LoadPackage

class FloatScreenResetDetector(
    private val graceWindowMs: Long = 700L,
    private val onReset: () -> Unit
) {
    private var lastFloatResumeObservedElapsed: Long = 0
    private val floatResetHandler = Handler(Looper.getMainLooper())
    private var pendingFloatResetCheck: Runnable? = null

    fun install(
        lpparam: XC_LoadPackage.LoadPackageParam,
        detachTarget: HookTarget?,
        resumeTarget: HookTarget?
    ) {
        if (detachTarget == null || resumeTarget == null) return

        installHookTargetObserver(
            lpparam = lpparam,
            target = detachTarget,
            onObserved = ::onFloatScreenDetachObserved
        )
        installHookTargetObserver(
            lpparam = lpparam,
            target = resumeTarget,
            onObserved = ::onFloatResumeObserved
        )
    }

    private fun onFloatScreenDetachObserved() {
        val detachObservedElapsed = SystemClock.elapsedRealtime()
        pendingFloatResetCheck?.let { floatResetHandler.removeCallbacks(it) }

        val check = Runnable {
            val resumeObservedElapsed = lastFloatResumeObservedElapsed
            val hasResumedAfterDetach = resumeObservedElapsed >= detachObservedElapsed
            val resumeElapsedSinceDetach = resumeObservedElapsed - detachObservedElapsed
            if (!hasResumedAfterDetach || resumeElapsedSinceDetach > graceWindowMs) {
                onReset()
            }
        }
        pendingFloatResetCheck = check
        floatResetHandler.postDelayed(check, graceWindowMs)
    }

    private fun onFloatResumeObserved() {
        lastFloatResumeObservedElapsed = SystemClock.elapsedRealtime()
    }

    private fun installHookTargetObserver(
        lpparam: XC_LoadPackage.LoadPackageParam,
        target: HookTarget,
        onObserved: () -> Unit
    ) {
        val paramTypes = resolveParamTypes(target.methodParams, lpparam) ?: return
        registerBeforeOrAfterObserver(
            lpparam = lpparam,
            target = target,
            paramTypes = paramTypes,
            onObserved = onObserved
        )
    }

    private fun registerBeforeOrAfterObserver(
        lpparam: XC_LoadPackage.LoadPackageParam,
        target: HookTarget,
        paramTypes: Array<Class<*>>,
        onObserved: () -> Unit
    ) {
        when (target.hookTiming?.lowercase()) {
            "before" -> lpparam.hookMethod(
                className = target.ownerClass,
                methodName = target.methodName,
                *paramTypes,
                before = { onObserved() }
            )

            "after" -> lpparam.hookMethod(
                className = target.ownerClass,
                methodName = target.methodName,
                *paramTypes,
                after = { onObserved() }
            )

            else -> Unit
        }
    }
}
