package com.niki914.nexus.agentic.mod

import android.content.Context
import com.niki914.nexus.h.util.ContextProvider

object HookLocalSettings {
    @Volatile
    private var cached = XSettings.LocalSettings()

    fun update(context: Context): XSettings.LocalSettings {
        return XService.getLocalSettings(context).also { cached = it }
    }

    suspend fun refreshFromHookContext(): XSettings.LocalSettings {
        val context = ContextProvider.await()
        return update(context)
    }

    fun current(): XSettings.LocalSettings = cached
}
