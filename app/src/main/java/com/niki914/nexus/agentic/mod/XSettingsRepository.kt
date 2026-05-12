package com.niki914.nexus.agentic.mod

import android.content.Context
import com.niki914.nexus.ipc.XIpcBridge

internal object XSettingsRepository {

    fun readWebSettings(context: Context): WebSettings {
        return WebSettings(parseJsonObject(XIpcBridge.readWebSettingsJson(context)))
    }

    fun writeWebSettingsJson(context: Context, json: String) {
        XIpcBridge.writeWebSettingsJson(context, json)
    }

    fun readLocalSettings(context: Context): LocalSettings {
        return LocalSettings(parseJsonObject(XIpcBridge.readLocalSettingsJson(context)))
    }

    fun writeLocalSettings(context: Context, settings: LocalSettings) {
        XIpcBridge.writeLocalSettingsJson(context, settings.props.toString())
    }
}