package com.niki914.nexus.agentic.app.ui.nexus.model

import com.niki914.nexus.agentic.app.ui.nexus.nav.HomePage
import com.niki914.nexus.agentic.app.ui.nexus.nav.NexusPage
import com.niki914.nexus.agentic.app.ui.nexus.nav.StartupPage
import com.niki914.nexus.agentic.repo.XRepo

data class AppLaunchDecision(
    val initialPage: NexusPage,
    val onboardingCompleted: Boolean,
    val endpointPresent: Boolean,
) {
    companion object {
        suspend fun resolve(
            startupAssistantUi: StartupAssistantUi,
        ): AppLaunchDecision {
            val onboardingCompleted = XRepo.onboardingCompleted()
            val endpointPresent = XRepo.llm().endpoint.isNotBlank()
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
            return AppLaunchDecision(
                initialPage = initialPage,
                onboardingCompleted = onboardingCompleted,
                endpointPresent = endpointPresent,
            )
        }
    }
}
