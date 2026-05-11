package com.niki914.nexus.ipc

import android.content.Context
import java.io.File

internal object ConfigPersistence {
    private const val FILE_NAME = "config.json"

    fun writeConfig(context: Context, json: String) {
        val file = File(context.filesDir, FILE_NAME)
        file.writeText(json)
    }

    fun readConfig(context: Context): String? {
        val file = File(context.filesDir, FILE_NAME)
        return if (file.exists()) file.readText() else null
    }
}
