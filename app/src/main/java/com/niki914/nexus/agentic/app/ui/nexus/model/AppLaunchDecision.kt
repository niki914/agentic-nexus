package com.niki914.nexus.agentic.app.ui.nexus.model

import com.niki914.nexus.agentic.app.ui.nexus.nav.HomePage
import com.niki914.nexus.agentic.app.ui.nexus.nav.NexusPage
import com.niki914.nexus.agentic.app.ui.nexus.nav.StartupPage
import com.niki914.nexus.agentic.mod.LocalSettings

data class AppLaunchDecision(
    val initialPage: NexusPage,
    val onboardingCompleted: Boolean,
    val endpointPresent: Boolean,
) {
    companion object {
        fun resolve(
            settings: LocalSettings,
            startupAssistantUi: StartupAssistantUi,
        ): AppLaunchDecision {
            val onboardingCompleted = settings.onboardingCompleted
            val endpointPresent = settings.endpoint.isNotBlank()
            val startupPage = when (startupAssistantUi) {
                StartupAssistantUi.Breeno,
                StartupAssistantUi.XiaoAi,
                StartupAssistantUi.ChatOnly -> StartupPage
            }
            val initialPage = if (onboardingCompleted && endpointPresent) {
                HomePage
            } else {
                startupPage
            }
            return AppLaunchDecision( // TODO remove
                initialPage = startupPage,
                onboardingCompleted = false,
                endpointPresent = endpointPresent
            )
            return AppLaunchDecision(
                initialPage = initialPage,
                onboardingCompleted = onboardingCompleted,
                endpointPresent = endpointPresent,
            )
        }
    }
}
