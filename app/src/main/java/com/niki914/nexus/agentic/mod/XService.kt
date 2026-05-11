package com.niki914.nexus.agentic.mod

import android.app.Application
import android.content.Context
import com.niki914.nexus.h.util.xlog
import com.niki914.nexus.ipc.XConfig

object XService {

    @Volatile
    private var cachedContext: Application? = null

    suspend fun refreshWebSettings(context: Context, packageName: String, versionCode: Long) {
        rememberContext(context)
        refreshWebSettingsInternal(context, packageName, versionCode)
    }

    fun getWebSettings(context: Context): XSettings.WebSettings {
        rememberContext(context)
        return readWebSettings(context)
    }

    fun getLocalSettings(context: Context): XSettings.LocalSettings {
        rememberContext(context)
        return readLocalSettings(context)
    }

    fun putLocalSettings(context: Context, settings: XSettings.LocalSettings) {
        rememberContext(context)
        writeLocalSettings(context, settings)
    }

    fun postNotification(
        title: String,
        content: String,
        uri: String?
    ) {
        val context = cachedContext?.applicationContext
        if (context == null) {
            xlog("XService.postNotification skipped: no cached context")
            return
        }
        XConfig.postNotification(context, title, content, uri)
    }

    private fun rememberContext(context: Context) {
        cachedContext = context.applicationContext as Application
    }
}
