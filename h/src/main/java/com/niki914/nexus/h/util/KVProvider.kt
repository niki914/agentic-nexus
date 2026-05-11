package com.niki914.nexus.h.util

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Remote settings!
 */
object KVProvider : XProvider<Map<String, JsonElement>>() {

    suspend fun getString(key: String): String? {
        return await()[key]?.jsonPrimitive?.contentOrNull
    }

    suspend fun getBoolean(key: String): Boolean? {
        return await()[key]?.jsonPrimitive?.booleanOrNull
    }

    suspend fun getInt(key: String): Int? {
        return await()[key]?.jsonPrimitive?.intOrNull
    }
    
    suspend fun getList(key: String): List<JsonElement>? {
        return await()[key]?.let {
            if (it is kotlinx.serialization.json.JsonArray) it.toList() else null
        }
    }
}