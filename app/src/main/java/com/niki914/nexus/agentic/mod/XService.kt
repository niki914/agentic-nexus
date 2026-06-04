package com.niki914.nexus.agentic.mod

import android.content.Context
import com.niki914.nexus.h.util.ContextProvider
import com.niki914.nexus.h.util.xlog
import com.niki914.nexus.ipc.XIpcBridge

object XService {

    suspend fun getLocalSettings(context: Context): LocalSettings {
        return LocalSettings(parseJsonObject(XIpcBridge.readLocalSettingsJson(context))).also {
            xlog("LocalSettings received: $it")
        }
    }

    suspend fun putLocalSettings(context: Context, settings: LocalSettings) {
        XIpcBridge.writeLocalSettingsJson(context, settings.props.toString())
        xlog("LocalSettings updated: ${settings.props}")
    }

    suspend fun postNotification(
        title: String,
        content: String,
        uri: String?
    ): Boolean {
        val context = ContextProvider.await()
        return XIpcBridge.postNotification(context, title, content, uri)
    }
}
