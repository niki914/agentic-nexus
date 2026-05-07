package com.niki914.breeno.a.mod

import android.content.Context
import com.niki914.breeno.h.util.xlog
import com.niki914.breeno.ipc.XConfig
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

// 伪造的服务端下发接口
fun getConfig(packageName: String, versionCode: Long): String {
    if (packageName == "com.heytap.speechassist") {
        return """
                    {
                        "package_name": "com.heytap.speechassist",
                        "config": {
                            "room_id_manager_class": "com.heytap.speechassist.aichat.AIChatRoomIdManager",
                            "room_id_manager_method_p": "p",
                            "view_bean_class": "com.heytap.speechassist.aichat.bean.AIChatViewBean",
                            "type_query_value": 1,
                            "type_answer_value": 2,
                            "data_center_class": "com.heytap.speechassist.aichat.AIChatDataCenter",
                            "data_center_method_r": "r",
                            "data_center_method_g1": "g1",
                            "mock_bean_methods_unit": [
                                ["setSkillType", "MyAI.StreamTextCard"],
                                ["setMsPerChar", 25],
                                ["setHasTextPrintAnimPlayed", false]
                            ],
                            "mock_bean_local_data_unit": [
                                ["MY_MOCK_FLAG", true],
                                ["bean_client_key_hide_feedback_view", true]
                            ],
                            "allowed_skill_types": []
                        }
                    }
                """.trimIndent()
    }
    return "{}"
}

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