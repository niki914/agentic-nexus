package com.niki914.nexus.agentic.mod.feat

import com.niki914.nexus.agentic.mod.XService
import com.niki914.nexus.h.util.ContextProvider
import com.niki914.nexus.h.util.xlog
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * 抽象的配置提供者基类，用于规范各个语音助手的配置包装器
 */
abstract class BaseConfigProvider { // TODO Dataclass to describe a hook spot
    private fun getElement(path: String): JsonElement? = runBlocking { // TODO 修改下游的读参方式，最好 suspend
        val config = XService.getWebSettings(ContextProvider.await()).config
        var current: JsonElement? = config?.get(path.substringBefore("."))
        path.substringAfter(".", "").takeIf { it.isNotEmpty() }?.split(".")?.forEach { key ->
            current = (current as? JsonObject)?.get(key)
        }
        if (current == null) {
            xlog("[BaseConfigProvider] Config missing or null for path: $path")
        }
        current
    }

    fun getString(path: String) = getElement(path)?.jsonPrimitive?.contentOrNull

    fun getBoolean(path: String) = getElement(path)?.jsonPrimitive?.booleanOrNull

    fun getInt(path: String) = getElement(path)?.jsonPrimitive?.intOrNull

    fun getList(path: String) = (getElement(path) as? JsonArray)?.toList()

    fun getObject(path: String) = getElement(path) as? JsonObject
}
