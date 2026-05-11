package com.niki914.nexus.h.util

import android.util.Log


internal fun xlogHookFailed(name: String, t: Throwable?) {
    xlog("Failed to hook $name\n${t?.stackTraceToString()}")
}

fun xtlog(tag: String, msg: String) = xlog("[$tag] $msg")

fun xlog(msg: String) = try {
    Log.e("Xlog", msg)
} catch (_: Throwable) {
    System.err.println("[xlog] $msg")
}