package com.niki914.nexus.ipc

import android.content.Context
import androidx.core.content.edit
import java.io.File

internal object ConfigPersistence {

    private const val WEB_SETTINGS_FILE_NAME = "config.json"
    private const val LOCAL_SETTINGS_PREFS = "x_service_local_settings"
    private const val LOCAL_SETTINGS_KEY = "local_settings_json"

    fun writeWebSettings(context: Context, json: String) {
        val file = File(context.filesDir, WEB_SETTINGS_FILE_NAME)
        file.writeText(json)
    }

    fun readWebSettings(context: Context): String? {
        val file = File(context.filesDir, WEB_SETTINGS_FILE_NAME)
        return if (file.exists()) file.readText() else null
    }

    fun writeLocalSettings(context: Context, json: String) {
        context.getSharedPreferences(LOCAL_SETTINGS_PREFS, Context.MODE_PRIVATE).edit {
            putString(LOCAL_SETTINGS_KEY, json)
        }
    }

    fun readLocalSettings(context: Context): String? {
        return context
            .getSharedPreferences(LOCAL_SETTINGS_PREFS, Context.MODE_PRIVATE)
            .getString(LOCAL_SETTINGS_KEY, null)
    }
}
