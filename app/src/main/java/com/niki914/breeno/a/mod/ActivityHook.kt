package com.niki914.breeno.a.mod

import android.app.Activity
import android.os.Bundle
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage
import com.niki914.breeno.h.core.runtime.Hook
import com.niki914.breeno.h.util.hookMethod
import com.niki914.breeno.h.util.xlog

class ActivityHook : Hook {
    override val name: String = "ActivityHook"

    override fun onHook(lpparam: XC_LoadPackage.LoadPackageParam) {
        lpparam.hookMethod(
            "android.app.Activity",
            "onCreate",
            Bundle::class.java,
            before = { param ->
                val activity = param.thisObject as Activity
                xlog("[$name] Activity onCreate: ${activity.javaClass.simpleName}")
                XposedBridge.log("[$name] Activity onCreate: ${activity.javaClass.simpleName}")
            }
        )
    }
}