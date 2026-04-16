package ${BASE_PACKAGE}.h.util

import android.app.Application
import ${BASE_PACKAGE}.h.core.runtime.Hook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Especially for context provider
 */
class ContextHook : Hook {
    override val name: String = "ContextHook"

    override fun onHook(lpparam: XC_LoadPackage.LoadPackageParam) {
        lpparam.hookMethod(
            "android.app.Application",
            "onCreate",
            after = { param ->
                val appContext = param.thisObject as Application
                val isFirstProvide = ContextProvider.provide(appContext)
                if (isFirstProvide) {
                    XposedBridge.log("[$name] Context successfully provided: ${appContext.packageName}")
                }
            }
        )
    }
}