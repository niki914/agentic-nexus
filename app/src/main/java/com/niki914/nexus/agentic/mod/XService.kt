package com.niki914.nexus.agentic.mod

import android.content.Context
import com.niki914.nexus.h.util.ContextProvider
import com.niki914.nexus.ipc.IpcReadResult
import com.niki914.nexus.ipc.IpcWriteResult
import com.niki914.nexus.ipc.XIpcBridge

object XService {

    suspend fun getLocalSettings(context: Context): LocalSettings {
        return when (val result = XIpcBridge.readLocalSettingsJson(context)) {
            is IpcReadResult.Success -> LocalSettings(parseJsonObject(result.json))
            is IpcReadResult.Unreachable -> LocalSettings()
            is IpcReadResult.NotFound -> LocalSettings()
        }
    }

    suspend fun putLocalSettings(context: Context, settings: LocalSettings) {
        XIpcBridge.writeLocalSettingsJson(context, settings.props.toString())
    }

    suspend fun postNotification(
        title: String,
        content: String,
        uri: String?
    ): Boolean {
        val context = ContextProvider.await()
        return XIpcBridge.postNotification(context, title, content, uri) is IpcWriteResult.Success
    }
}
