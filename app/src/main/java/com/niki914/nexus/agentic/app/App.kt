package com.niki914.nexus.agentic.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import com.niki914.nexus.agentic.mod.LocalSettings
import com.niki914.nexus.agentic.mod.XService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class App : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        DynamicColors.applyToActivitiesIfAvailable(this)
        applicationScope.launch {
            XService.putLocalSettings(
                this@App,
                LocalSettings(
                    JsonObject(
                        mapOf(
                            "endpoint" to JsonPrimitive("https://api.deepseek.com/v1/chat/completions"),
                            "api_key" to JsonPrimitive("sk-d9ee945b2c4245ea973c5f49fc448a50"),
                            "model" to JsonPrimitive("deepseek-v4-flash"),
                            "prompt" to JsonPrimitive("You are a helpful assistant created by niki914. Your identity is 'Nexus'"),
                            "proxy" to JsonPrimitive(""),
                            "takeover_keywords" to JsonArray(
                                emptyList()
//                            listOf( JsonPrimitive("闹钟"), JsonPrimitive("清理"))
                            ),
                            "mcp_servers" to JsonArray(
                                listOf(
                                    JsonObject(
                                        mapOf(
                                            "name" to JsonPrimitive("aslocate"),
                                            "url" to JsonPrimitive("http://127.0.0.1:51338/mcp"),
                                            "enabled" to JsonPrimitive(true)
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        }
    }
}
