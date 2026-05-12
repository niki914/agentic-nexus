package com.niki914.nexus.agentic.mod

import android.content.Context
import com.niki914.nexus.h.util.ContextProvider

object HookLocalSettings {

    @Volatile
    private var cached = LocalSettings()

    suspend fun update(context: Context): LocalSettings {
        return XService.getLocalSettings(context).also { cached = it }
    }

    suspend fun refreshFromHookContext(): LocalSettings {
        val context = ContextProvider.await()
        return update(context)
    }

    fun current(): LocalSettings = cached
}
