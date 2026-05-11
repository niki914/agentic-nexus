package com.niki914.nexus.agentic.mod

import android.content.Context
import com.niki914.nexus.h.util.xTry
import com.niki914.nexus.h.util.xlog
import com.niki914.nexus.ipc.XConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import androidx.core.content.edit

sealed class XSettings(
    val props: JsonObject
) {
    class WebSettings(props: JsonObject = JsonObject(emptyMap())) : XSettings(props) {
        val packageName: String
            get() = getString("package_name")

        val config: JsonObject?
            get() = getObject("config")
    }

    class LocalSettings(props: JsonObject = JsonObject(emptyMap())) : XSettings(props) {
        val endpoint: String
            get() = getString("endpoint")

        val apiKey: String
            get() = getString("api_key")

        val model: String
            get() = getString("model")

        val prompt: String
            get() = getString("prompt")

        val proxy: String
            get() = getString("proxy")

        val takeoverKeywords: List<String>
            get() = getStringList("takeover_keywords")
    }

    fun getString(key: String, default: String = ""): String =
        props[key]?.jsonPrimitive?.contentOrNull ?: default

    fun getBoolean(key: String, default: Boolean = false): Boolean =
        props[key]?.jsonPrimitive?.booleanOrNull ?: default

    fun getInt(key: String, default: Int = 0): Int =
        props[key]?.jsonPrimitive?.intOrNull ?: default

    fun getStringList(key: String): List<String> =
        props[key]
            ?.jsonArray
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }
            ?: emptyList()

    fun getObject(key: String): JsonObject? =
        props[key]?.jsonObject

    fun getArray(key: String): JsonArray? =
        props[key]?.jsonArray

    fun getElement(key: String): JsonElement? =
        props[key]
}

fun Context.getCachedSettings(): XSettings.WebSettings = XService.getWebSettings(this)

suspend fun Context.refreshWebSettings(packageName: String, versionCode: Long) {
    XService.refreshWebSettings(this, packageName, versionCode)
}

internal fun readWebSettings(context: Context): XSettings.WebSettings =
    (xTry("XService.getWebSettings") {
        XSettings.WebSettings(parseJsonObject(XConfig.get(context)))
    } ?: XSettings.WebSettings()).also {
        xlog("WebSettings received: $it")
    }

internal fun readLocalSettings(context: Context): XSettings.LocalSettings =
    (xTry("XService.getLocalSettings") {
        val jsonStr = context
            .getSharedPreferences(LOCAL_SETTINGS_PREFS, Context.MODE_PRIVATE)
            .getString(LOCAL_SETTINGS_KEY, null)
        XSettings.LocalSettings(parseJsonObject(jsonStr))
    } ?: XSettings.LocalSettings()).also {
        xlog("LocalSettings received: $it")
    }

internal fun writeLocalSettings(context: Context, settings: XSettings.LocalSettings) {
    context
        .getSharedPreferences(LOCAL_SETTINGS_PREFS, Context.MODE_PRIVATE)
        .edit {
            putString(LOCAL_SETTINGS_KEY, settings.props.toString())
        }
    xlog("LocalSettings updated: ${settings.props}")
}

internal suspend fun refreshWebSettingsInternal(
    context: Context,
    packageName: String,
    versionCode: Long
) = withContext(Dispatchers.IO) {
    val remoteJson = fetchWebSettingsSync(packageName, versionCode)
    if (remoteJson != null) {
        XConfig.updateFromServerJson(context, remoteJson)
        xlog("WebSettings refreshed for $packageName ($versionCode)")
    } else {
        xlog("WebSettings refresh failed for $packageName ($versionCode): remote JSON is null")
    }
}

private fun parseJsonObject(jsonString: String?): JsonObject {
    if (jsonString.isNullOrBlank()) {
        return JsonObject(emptyMap())
    }
    return xTry("parseJsonObject") {
        json.parseToJsonElement(jsonString).jsonObject
    } ?: JsonObject(emptyMap())
}

private fun fetchWebSettingsSync(packageName: String, versionCode: Long): String? {
    val url = buildWebSettingsUrl(packageName, versionCode)
    val request = Request.Builder()
        .url(url)
        .build()
    return xTry("fetchWebSettingsSync") {
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful || response.body == null) {
                xlog("fetchWebSettingsSync failed for $url: code=${response.code}")
                return@xTry null
            }
            response.body?.string()
        }
    }
}

private fun buildWebSettingsUrl(packageName: String, versionCode: Long): String {
    val host = "127.0.0.1:8788"
    val alias = if (packageName == "com.heytap.speechassist") "breeno" else packageName // TODO 重构改为直接用包名｜涉及 server
    return "http://$host/$alias/$versionCode/config.json"
}

private const val LOCAL_SETTINGS_PREFS = "x_service_local_settings"
private const val LOCAL_SETTINGS_KEY = "local_settings_json"
private val json = Json { ignoreUnknownKeys = true }
private val httpClient = OkHttpClient()