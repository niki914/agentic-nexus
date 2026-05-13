package com.niki914.nexus.agentic.mod

import com.niki914.nexus.h.util.xTry
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

abstract class XSettings(
    val props: JsonObject
) {
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

class WebSettings(props: JsonObject = JsonObject(emptyMap())) : XSettings(props) {
    val packageName: String // TODO remove 反正可以直接用 lpparam 拿到
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

internal fun parseJsonObject(jsonString: String?): JsonObject {
    if (jsonString.isNullOrBlank()) {
        return JsonObject(emptyMap())
    }
    return xTry("parseJsonObject") {
        json.parseToJsonElement(jsonString).jsonObject
    } ?: JsonObject(emptyMap())
}

private val json = Json { ignoreUnknownKeys = true }