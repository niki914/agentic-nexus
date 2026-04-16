package ${BASE_PACKAGE}.h.util

import ${BASE_PACKAGE}.h.core.runtime.Hook
import de.robv.android.xposed.callbacks.XC_LoadPackage
import kotlinx.coroutines.CoroutineScope

object HookSideLoader {

    fun load(
        scope: CoroutineScope,
        hook: Hook,
        lpparam: XC_LoadPackage.LoadPackageParam
    ) {
        hook.onHook(lpparam)
    }
}