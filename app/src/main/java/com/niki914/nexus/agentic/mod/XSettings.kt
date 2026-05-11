package com.niki914.nexus.agentic.mod

import android.content.Context
import com.niki914.nexus.h.util.xTry
import com.niki914.nexus.h.util.xlog
import com.niki914.nexus.ipc.XConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request


fun Context.getCachedSettings(): XSettings.WebSettings = (xTry("getCachedSettings") {
    val jsonStr = XConfig.get(this)
    if (jsonStr.isBlank()) return@xTry XSettings.WebSettings(JsonObject(emptyMap()))
    XSettings.WebSettings(json.decodeFromString<JsonObject>(jsonStr))
} ?: XSettings.WebSettings(JsonObject(emptyMap()))).also {
    xlog("LocalSettings received: $it")
}

suspend fun Context.refreshWebSettings(packageName: String, versionCode: Long) = withContext(Dispatchers.IO) {
    val remoteJson = fetchWebSettingsSync(packageName, versionCode)
    if (remoteJson != null) {
        XConfig.updateFromServerJson(this@refreshWebSettings, remoteJson)
        xlog("LocalSettings refreshed from remote for $packageName ($versionCode): $remoteJson")
    } else {
        xlog("LocalSettings refresh failed for $packageName ($versionCode): remote JSON is null")
    }
}

sealed class XSettings(
    val props: JsonObject // 支持自由键值对
) {
    class WebSettings(props: JsonObject) : XSettings(props)

    class LocalSettings(props: JsonObject) : XSettings(props)

    companion object {
        fun buildUrl(packageName: String, versionCode: Long): String {
            // 保持与 server.py 目录结构一致: {packageName}/{versionCode}/config.json
            // 注意：packageName 里的点号在 URL 中是正常的，但建议在 server 端对应目录名
            val host = "127.0.0.1:8788"
            // 简单处理：如果是 com.heytap.speechassist，对应目录名为 breeno
            val alias = if (packageName == "com.heytap.speechassist") "breeno" else packageName
            return "http://$host/$alias/$versionCode/config.json"
        }
    }
    // ... 保持其他方法不变
    // 封装便捷的取值方法，抹平读取 JsonElement 时繁琐的 .jsonPrimitive 强转
    fun getString(key: String, default: String = "") =
        props[key]?.jsonPrimitive?.contentOrNull ?: default

    fun getBoolean(key: String, default: Boolean = false) =
        props[key]?.jsonPrimitive?.booleanOrNull ?: default

    fun getInt(key: String, default: Int = 0) =
        props[key]?.jsonPrimitive?.intOrNull ?: default

    fun getObject(key: String): JsonObject? =
        props[key]?.jsonObject

    fun getElement(key: String): JsonElement? =
        props[key]
}

private val json = Json { ignoreUnknownKeys = true }

fun fetchWebSettingsSync(packageName: String, versionCode: Long): String? {
    val client = OkHttpClient()
    val url = XSettings.buildUrl(packageName, versionCode)
    val request = Request.Builder()
        .url(url)
        .build()
    return xTry {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful || response.body == null) {
                xlog("fetchWebSettingsSync failed for $url: code: ${response.code}")
                return@xTry null
            }
            response.body?.string()
        }
    }
}
