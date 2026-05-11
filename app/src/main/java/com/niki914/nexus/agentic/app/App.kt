package com.niki914.nexus.agentic.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import com.niki914.nexus.agentic.mod.LocalSettings
import com.niki914.nexus.agentic.mod.XService
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        DynamicColors.applyToActivitiesIfAvailable(this)
        XService.putLocalSettings(
            this,
            LocalSettings(
                JsonObject(
                    mapOf(
                        "endpoint" to JsonPrimitive("https://api.deepseek.com/v1/chat/completions"),
                        "api_key" to JsonPrimitive("sk-xxx"),
                        "model" to JsonPrimitive("deepseek-v4-flash"),
                        "prompt" to JsonPrimitive("You are a helpful assistant."),
                        "proxy" to JsonPrimitive(""),
                        "takeover_keywords" to JsonArray(listOf( JsonPrimitive("闹钟"), JsonPrimitive("清理")))
                    )
                )
            )
        )
    }
}