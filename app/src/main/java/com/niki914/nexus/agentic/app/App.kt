package com.niki914.nexus.agentic.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import com.niki914.nexus.agentic.mod.LocalSettings
import com.niki914.nexus.agentic.mod.XService
import com.niki914.nexus.h.util.xlog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class App : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        DynamicColors.applyToActivitiesIfAvailable(this)
        applicationScope.launch {
            val existingSettings = XService.getLocalSettings(this@App)
            if (!shouldSeedDefaultSettings(existingSettings)) {
                return@launch
            }
            XService.putLocalSettings(
                this@App,
                mergeDefaultLocalSettings(existingSettings),
            )
        }
    }

    private fun shouldSeedDefaultSettings(settings: LocalSettings): Boolean {
        return true
        return settings.endpoint.isBlank() &&
            settings.model.isBlank() &&
            settings.customTools == null
    }

    private fun mergeDefaultLocalSettings(existingSettings: LocalSettings): LocalSettings {
        val mergedProps = existingSettings.props.toMutableMap()
        buildDefaultProps().forEach { (key, value) ->
            if (shouldApplyDefaultValue(existingSettings, key)) {
                mergedProps[key] = value
                xlog("mergeDefaultLocalSettings put: $key, value: $value")
            } else {
                xlog("mergeDefaultLocalSettings skip putting $key, curr value: ${existingSettings.apiKey}")
            }
        }
        return LocalSettings(JsonObject(mergedProps))
    }

    private fun shouldApplyDefaultValue(
        settings: LocalSettings,
        key: String,
    ): Boolean {
        return when (key) {
            "endpoint" -> settings.endpoint.isBlank()
            "api_key" -> settings.apiKey.isBlank()
            "model" -> settings.model.isBlank()
            "prompt" -> settings.prompt.isBlank()
            "proxy" -> settings.proxy.isBlank()
            "takeover_keywords" -> settings.takeoverKeywords.isEmpty()
            "mcp_servers" -> settings.mcpServers == null
            "custom_tools" -> settings.customTools == null
            else -> settings.props[key] == null
        }
    }

    private fun buildDefaultProps(): Map<String, JsonElement> {
        return mapOf(
            "endpoint" to JsonPrimitive("https://api.xiaomimimo.com/v1/chat/completions"),
            "api_key" to JsonPrimitive("sk-c8tr0a4hj0kbosmda83j4plbob6a5pe1xm8h20246r2einvf"),
            "model" to JsonPrimitive("mimo-v2.5"),
            "prompt" to JsonPrimitive("You are a helpful assistant created by niki914. Your identity is 'Nexus'"),
            "proxy" to JsonPrimitive(""),
            "takeover_keywords" to JsonArray(
                emptyList()
//              listOf(JsonPrimitive("闹钟"), JsonPrimitive("清理"))
            ),
//            "custom_tools" to JsonArray(
//                listOf(
//                    JsonObject(
//                        mapOf(
//                            "name" to JsonPrimitive("device_model"),
//                            "description" to JsonPrimitive("读取当前设备型号"),
//                            "enabled" to JsonPrimitive(true),
//                            "command" to JsonPrimitive("getprop ro.product.model")
//                        )
//                    )
//                )
//            ),
//            "mcp_servers" to JsonArray(
//                listOf(
//                    JsonObject(
//                        mapOf(
//                            "name" to JsonPrimitive("aslocate"),
//                            "url" to JsonPrimitive("http://127.0.0.1:51338/mcp"),
//                            "enabled" to JsonPrimitive(true)
//                        )
//                    )
//                )
//            )
        )
    }
}
