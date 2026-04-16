package ${BASE_PACKAGE}.a.mod

import android.content.Context
import ${BASE_PACKAGE}.ipc.XConfig
import ${BASE_PACKAGE}.h.util.xlog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

fun Context.getLocalSettings(): XSettings? = try {
    XConfig.get(this).toXSettings().also {
        xlog("LocalSettings received: $it")
    }
} catch (t: Throwable) {
    xlog("LocalSettings received failed: ${t.stackTraceToString()}")
    null
}

suspend fun Context.refreshLocalSettings() = withContext(Dispatchers.IO) {
    val json = fetchXSettingsSync() ?: return@withContext
    XConfig.updateFromServerJson(this@refreshLocalSettings, json)
    xlog("LocalSettings refreshed: $json")
}

data class XSettings(
    val props: JsonObject // 支持自由键值对
) {
    // 封装便捷的取值方法，抹平读取 JsonElement 时繁琐的 .jsonPrimitive 强转
    fun getString(key: String, default: String = "") =
        props[key]?.jsonPrimitive?.contentOrNull ?: default

    fun getBoolean(key: String, default: Boolean = false) =
        props[key]?.jsonPrimitive?.booleanOrNull ?: default

    fun getInt(key: String, default: Int = 0) =
        props[key]?.jsonPrimitive?.intOrNull ?: default
}

private val json = Json { ignoreUnknownKeys = true }

fun String.toXSettings(): XSettings {
    if (this.isBlank()) return XSettings(JsonObject(emptyMap()))

    return runCatching {
        XSettings(json.decodeFromString<JsonObject>(this))
    }.onFailure {
        xlog("toXSettings failed: ${it.stackTraceToString()}")
    }.getOrDefault(XSettings(JsonObject(emptyMap())))
}

fun fetchXSettingsSync(): String? {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url(Entrance.SETTINGS_URL)
        .build()
    return runCatching {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful || response.body == null) {
                xlog("fetchXSettingsSync failed: code: ${response.code}, body: ${response.body?.string()}")
                return null
            }
            return response.body?.string()
        }
    }.getOrNull()
}