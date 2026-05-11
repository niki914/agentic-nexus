package com.niki914.nexus.h.util

import de.robv.android.xposed.callbacks.XC_LoadPackage

fun <T> XC_LoadPackage.LoadPackageParam.xTry(
    name: String = "xTry",
    onError: ((Throwable?) -> Unit)? = null,
    block: () -> T
): T? = xTryInternal(this, name, onError, block)

fun <T> xTry(
    name: String = "xTry",
    block: () -> T
): T? = xTryInternal(null, name, null, block)

private fun <T> xTryInternal(
    lpparam: XC_LoadPackage.LoadPackageParam?,
    name: String,
    onError: ((Throwable?) -> Unit)? = null,
    block: () -> T
): T? = runCatching(block).onFailure {
    xlogHookFailed(name, it)
    val className = name.substringBefore('#').substringAfter(':')
    if (className.isNotBlank()) lpparam?.inspectClass(className)
    onError?.invoke(it)
}.getOrNull()