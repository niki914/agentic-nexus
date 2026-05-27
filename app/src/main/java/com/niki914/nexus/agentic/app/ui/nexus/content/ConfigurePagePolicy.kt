package com.niki914.nexus.agentic.app.ui.nexus.content

import com.niki914.nexus.agentic.app.ui.nexus.model.ProviderSpec

data class ConfigurePagePolicy(
    val showEndpointSection: Boolean,
    val showEndpointOverrideToggle: Boolean,
)

internal fun onboardingConfigurePolicy(providerSpec: ProviderSpec): ConfigurePagePolicy {
    return ConfigurePagePolicy(
        showEndpointSection = providerSpec.showEndpointConfigInOnboarding,
        showEndpointOverrideToggle = providerSpec.showEndpointConfigInOnboarding,
    )
}
