package com.niki914.nexus.agentic.app.ui.nexus.content

import com.niki914.nexus.agentic.app.ui.nexus.model.ConfigureScene
import com.niki914.nexus.agentic.app.ui.nexus.model.ProviderSpec

data class ConfigurePagePolicy(
    val showEndpointSection: Boolean,
    val showEndpointOverrideToggle: Boolean,
    val endpointEditable: Boolean,
    val showAdvancedSection: Boolean,
)

internal fun onboardingConfigurePolicy(providerSpec: ProviderSpec): ConfigurePagePolicy {
    return ConfigurePagePolicy(
        showEndpointSection = providerSpec.showEndpointConfigInOnboarding,
        showEndpointOverrideToggle = providerSpec.showEndpointConfigInOnboarding,
        endpointEditable = providerSpec.showEndpointConfigInOnboarding,
        showAdvancedSection = false,
    )
}

internal fun configurePagePolicy(
    scene: ConfigureScene,
    providerSpec: ProviderSpec,
): ConfigurePagePolicy {
    return when (scene) {
        ConfigureScene.Onboarding -> onboardingConfigurePolicy(providerSpec)
        ConfigureScene.Settings -> ConfigurePagePolicy(
            showEndpointSection = true,
            showEndpointOverrideToggle = false,
            endpointEditable = true,
            showAdvancedSection = true,
        )
    }
}
