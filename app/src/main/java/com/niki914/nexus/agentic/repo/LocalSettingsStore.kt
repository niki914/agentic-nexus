package com.niki914.nexus.agentic.repo

import android.content.Context
import com.niki914.nexus.agentic.mod.LocalSettings
import com.niki914.nexus.agentic.mod.XService

internal interface LocalSettingsStore {
    suspend fun read(context: Context): LocalSettings

    suspend fun write(context: Context, settings: LocalSettings)
}

internal object XServiceLocalSettingsStore : LocalSettingsStore {
    override suspend fun read(context: Context): LocalSettings {
        return XService.getLocalSettings(context)
    }

    override suspend fun write(context: Context, settings: LocalSettings) {
        XService.putLocalSettings(context, settings)
    }
}
