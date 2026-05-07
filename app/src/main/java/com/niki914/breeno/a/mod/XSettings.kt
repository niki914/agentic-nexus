package com.niki914.breeno.a.mod

import android.content.Context
import com.niki914.breeno.h.util.xTry
import com.niki914.breeno.h.util.xlog
import com.niki914.breeno.ipc.XConfig
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

private fun getMockConfig(packageName: String, versionCode: Long): String {
    if (packageName == "com.heytap.speechassist") {
        return """
                    {
                        "package_name": "com.heytap.speechassist",
                        "config_version": 2,
                        "config": {
                            "classes": {
                                "room_id_manager": "com.heytap.speechassist.aichat.AIChatRoomIdManager",
                                "view_bean": "com.heytap.speechassist.aichat.bean.AIChatViewBean",
                                "data_center": "com.heytap.speechassist.aichat.AIChatDataCenter",
                                "feedback_info": "com.heytap.speech.engine.protocol.directive.tracking.BreenoFeedback",
                                "footer_info": "com.heytap.speech.engine.protocol.directive.tracking.FooterInfo"
                            },
                            "accessors": {
                                "room_id_manager": {
                                    "create_room": "p"
                                },
                                "bean": {
                                    "get_chat_type": "getChatType",
                                    "set_chat_type": "setChatType",
                                    "get_skill_type": "getSkillType",
                                    "get_room_id": "getRoomId",
                                    "set_room_id": "setRoomId",
                                    "get_content": "getContent",
                                    "set_content": "setContent",
                                    "set_record_id": "setRecordId",
                                    "set_final": "setFinal",
                                    "set_first_slice": "setFirstSlice",
                                    "set_feedback_info": "setFeedBackInfo",
                                    "add_client_local_data": "addClientLocalData",
                                    "get_client_local_data": "getClientLocalData"
                                },
                                "feedback": {
                                    "set_footer_info": "setFooterInfo"
                                },
                                "footer_info": {
                                    "set_copy_flag": "setCopyFlag",
                                    "set_upvote_flag": "setUpvoteFlag"
                                },
                                "data_center": {
                                    "insert_message": "r",
                                    "update_message": "g1"
                                }
                            },
                            "schema": {
                                "chat_type": {
                                    "query": 1,
                                    "answer": 2
                                },
                                "mock_flags": {
                                    "self_injected": "MY_MOCK_FLAG",
                                    "hide_feedback_view": "bean_client_key_hide_feedback_view"
                                }
                            },
                            "runtime": {
                                "answer_skill_policy": {
                                    "mode": "whitelist",
                                    "types": []
                                },
                                "feedback_defaults": {
                                    "copy_flag": true,
                                    "upvote_flag": true
                                },
                                "mock_defaults": {
                                    "bean_methods": {
                                        "setSkillType": "MyAI.StreamTextCard",
                                        "setBasicContextView": true,
                                        "setModeType": -1,
                                        "setShowStatement": true,
                                        "setStatement": "> Powered By niki914",
                                        "setShowTTSPlayIcon": false,
                                        "setMsPerChar": 25,
                                        "setHasTextPrintAnimPlayed": false
                                    },
                                    "local_data": {
                                        "MY_MOCK_FLAG": true,
                                        "fromcui": true,
                                        "resultStart": true,
                                        "key_dash_line": true,
                                        "bean_client_key_hide_feedback_view": true
                                    }
                                }
                            }
                        }
                    }
                """.trimIndent()
    }
    return "{}"
}

private fun buildMockSettings(packageName: String, versionCode: Long): XSettings {
    val mockJson = xTry {
        json.decodeFromString<JsonObject>(getMockConfig(packageName, versionCode))
    } ?: JsonObject(emptyMap())

    return XSettings(
        JsonObject(mockJson)
    )
}

fun Context.getLocalSettings(): XSettings? = xTry("getLocalSettings") {
    XConfig.get(this).toXSettings().also {
        xlog("LocalSettings received: $it")
    }
}

suspend fun Context.refreshLocalSettings() = withContext(Dispatchers.IO) {
    val remoteJson = fetchXSettingsSync()
    if (remoteJson != null) {
        XConfig.updateFromServerJson(this@refreshLocalSettings, remoteJson)
        xlog("LocalSettings refreshed from remote: $remoteJson")
    } else {
        xlog("LocalSettings refresh failed: remote JSON is null")
    }
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

    fun getObject(key: String): JsonObject? =
        props[key]?.jsonObject

    fun getElement(key: String): JsonElement? =
        props[key]
}

private val json = Json { ignoreUnknownKeys = true }

fun String.toXSettings(): XSettings {
    if (this.isBlank()) return XSettings(JsonObject(emptyMap()))

    return xTry {
        XSettings(json.decodeFromString<JsonObject>(this))
    } ?: XSettings(JsonObject(emptyMap()))
}

fun fetchXSettingsSync(): String? {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url(Entrance.SETTINGS_URL)
        .build()
    return xTry {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful || response.body == null) {
                xlog("fetchXSettingsSync failed: code: ${response.code}, body: ${response.body?.string()}")
                return@xTry null
            }
            response.body?.string()
        }
    }
}
