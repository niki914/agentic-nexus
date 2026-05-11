package com.niki914.nexus.h.util

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage


fun XC_LoadPackage.LoadPackageParam.findClass(name: String): Class<*>? =
    xTry(name) { XposedHelpers.findClass(name, this.classLoader) }

fun XC_LoadPackage.LoadPackageParam.hookMethod(
    className: String,
    methodName: String,
    vararg params: Any?,
    before: (XC_MethodHook.MethodHookParam) -> Unit = {},
    after: (XC_MethodHook.MethodHookParam) -> Unit = {},
    onError: ((Throwable?) -> Unit)? = null
) {
    val hookName = "$className#$methodName"
    xTry(hookName, onError) {
        val clazz = findClass(className) ?: return@xTry
        val hookParams = arrayOf(*params, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                xTry(hookName, onError) { before(param) }
            }

            override fun afterHookedMethod(param: MethodHookParam) {
                xTry(hookName, onError) { after(param) }
            }
        })
        XposedHelpers.findAndHookMethod(clazz, methodName, *hookParams)
    }
}

fun XC_LoadPackage.LoadPackageParam.hookConstructor(
    className: String,
    vararg params: Any?,
    before: (XC_MethodHook.MethodHookParam) -> Unit = {},
    after: (XC_MethodHook.MethodHookParam) -> Unit = {},
    onError: ((Throwable?) -> Unit)? = null
) {
    val hookName = "$className#CONSTRUCTOR"
    xTry(hookName, onError) {
        val clazz = findClass(className) ?: return@xTry
        val hookParams = arrayOf(*params, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                xTry(hookName, onError) { before(param) }
            }

            override fun afterHookedMethod(param: MethodHookParam) {
                xTry(hookName, onError) { after(param) }
            }
        })
        XposedHelpers.findAndHookConstructor(clazz, *hookParams)
    }
}

fun hookKey(
    stage: String,
    className: String,
    methodName: String,
    paramTypes: List<Class<*>>
): String = xTry("hookKey:$className#$methodName") {
    "$stage:$className#$methodName(${paramTypes.joinToString(",") { it.name }})"
} ?: ""

fun constructorHookKey(
    stage: String,
    className: String,
    paramTypes: List<Class<*>>
): String = xTry("constructorHookKey:$className") {
    "$stage:$className#CONSTRUCTOR(${paramTypes.joinToString(",") { it.name }})"
} ?: ""
