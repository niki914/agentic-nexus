package com.niki914.nexus.agentic.runtime.settings

object RuntimeEnvironment {
    @Volatile
    private var settingsGateway: RuntimeSettingsGateway? = null

    fun install(settingsGateway: RuntimeSettingsGateway) {
        this.settingsGateway = settingsGateway
    }

    fun requireSettingsGateway(): RuntimeSettingsGateway {
        return settingsGateway
            ?: error("RuntimeSettingsGateway is not installed.")
    }

    fun clearForTest() {
        settingsGateway = null
    }
}
