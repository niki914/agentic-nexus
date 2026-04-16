package ${BASE_PACKAGE}.h.util

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage


fun XC_LoadPackage.LoadPackageParam.findClass(name: String): Class<*> =
    XposedHelpers.findClass(name, this.classLoader)

fun XC_LoadPackage.LoadPackageParam.hookMethod(
    className: String,
    methodName: String,
    vararg params: Any?,
    before: (XC_MethodHook.MethodHookParam) -> Unit = {},
    after: (XC_MethodHook.MethodHookParam) -> Unit = {},
    onError: ((Throwable?) -> Unit)? = null
) {
    try {
        val clazz = findClass(className)
        val hookParams = arrayOf(*params, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                runCatching { before(param) }
                    .onFailure {
                        xlogHookFailed("$className#$methodName", it)
                        inspectClass(className)
                        onError?.invoke(it)
                    }
            }

            override fun afterHookedMethod(param: MethodHookParam) {
                runCatching { after(param) }
                    .onFailure {
                        xlogHookFailed("$className#$methodName", it)
                        inspectClass(className)
                        onError?.invoke(it)
                    }
            }
        })
        XposedHelpers.findAndHookMethod(clazz, methodName, *hookParams)
    } catch (t: Throwable) {
        xlogHookFailed("$className#$methodName", t)
        inspectClass(className)
        onError?.invoke(t)
    }
}

/**
 * Helper for constructor hooks.
 */
fun XC_LoadPackage.LoadPackageParam.hookConstructor(
    className: String,
    vararg params: Any?,
    before: (XC_MethodHook.MethodHookParam) -> Unit = {},
    after: (XC_MethodHook.MethodHookParam) -> Unit = {},
    onError: ((Throwable?) -> Unit)? = null
) {
    try {
        val clazz = findClass(className)
        val hookParams = arrayOf(*params, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                runCatching { before(param) }
                    .onFailure {
                        xlogHookFailed("$className#CONSTRUCTOR", it)
                        inspectClass(className)
                        onError?.invoke(it)
                    }
            }

            override fun afterHookedMethod(param: MethodHookParam) {
                runCatching { after(param) }
                    .onFailure {
                        xlogHookFailed("$className#CONSTRUCTOR", it)
                        inspectClass(className)
                        onError?.invoke(it)
                    }
            }
        })
        XposedHelpers.findAndHookConstructor(clazz, *hookParams)
    } catch (t: Throwable) {
        xlogHookFailed("$className#CONSTRUCTOR", t)
        inspectClass(className)
        onError?.invoke(t)
    }
}

fun hookKey(
    stage: String,
    className: String,
    methodName: String,
    paramTypes: List<Class<*>>
): String {
    return "$stage:$className#$methodName(${paramTypes.joinToString(",") { it.name }})"
}

fun constructorHookKey(
    stage: String,
    className: String,
    paramTypes: List<Class<*>>
): String {
    return "$stage:$className#CONSTRUCTOR(${paramTypes.joinToString(",") { it.name }})"
}
