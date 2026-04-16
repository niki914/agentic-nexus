package ${BASE_PACKAGE}.h.util

import android.util.Log


internal fun xlogHookFailed(name: String, t: Throwable?) {
    xlog("Failed to hook $name\n${t?.stackTraceToString()}")
}

fun xtlog(tag: String, msg: String) = xlog("[$tag] $msg")

fun xlog(msg: String) {
    runCatching {
        Log.e("Xlog", msg)
    }
}