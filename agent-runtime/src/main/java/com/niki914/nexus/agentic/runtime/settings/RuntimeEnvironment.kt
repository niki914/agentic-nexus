package com.niki914.nexus.agentic.runtime.settings

import kotlinx.coroutines.CompletableDeferred

object RuntimeEnvironment {
    @Volatile
    private var settingsGateway: RuntimeSettingsGateway? = null
    @Volatile
    private var settingsGatewayReady = CompletableDeferred<RuntimeSettingsGateway>()

    fun install(settingsGateway: RuntimeSettingsGateway) {
        this.settingsGateway = settingsGateway
        if (!settingsGatewayReady.isCompleted) {
            settingsGatewayReady.complete(settingsGateway)
        }
    }

    fun requireSettingsGateway(): RuntimeSettingsGateway {
        return settingsGateway
            ?: error("RuntimeSettingsGateway is not installed.")
    }

    suspend fun awaitSettingsGateway(): RuntimeSettingsGateway {
        settingsGateway?.let { return it }
        return settingsGatewayReady.await()
    }

    fun clearForTest() {
        settingsGateway = null
        settingsGatewayReady = CompletableDeferred()
    }
}
