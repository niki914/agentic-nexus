package com.niki914.nexus.agentic.app.ui.nexus

import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.niki914.nexus.agentic.app.R
import com.niki914.nexus.agentic.app.ui.infra.nav.NavigationEntry
import com.niki914.nexus.agentic.mod.LocalSettings
import com.niki914.nexus.agentic.mod.XService
import com.niki914.nexus.agentic.app.ui.nexus.content.ConfigurePageContent
import com.niki914.nexus.agentic.app.ui.nexus.content.HomePageContent
import com.niki914.nexus.agentic.app.ui.nexus.content.SelectionOption
import com.niki914.nexus.agentic.app.ui.nexus.content.SelectionPageContent
import com.niki914.nexus.agentic.app.ui.nexus.content.SettingsDetailPageContent
import com.niki914.nexus.agentic.app.ui.nexus.content.SettingsHomePageContent
import com.niki914.nexus.agentic.app.ui.nexus.content.StartupPageContent
import com.niki914.nexus.agentic.app.ui.nexus.model.StartupAssistantUi
import com.niki914.nexus.agentic.app.ui.nexus.nav.ConfigurePage
import com.niki914.nexus.agentic.app.ui.nexus.nav.DonePage
import com.niki914.nexus.agentic.app.ui.nexus.nav.HomePage
import com.niki914.nexus.agentic.app.ui.nexus.nav.NexusPage
import com.niki914.nexus.agentic.app.ui.nexus.nav.ProviderPickPage
import com.niki914.nexus.agentic.app.ui.nexus.nav.SettingsDetailPage
import com.niki914.nexus.agentic.app.ui.nexus.nav.SettingsHomePage
import com.niki914.nexus.agentic.app.ui.nexus.nav.StartupPage
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive

@Composable
fun NexusPageContent(
    entry: NavigationEntry<NexusPage>,
    topPadding: Dp,
    hazeState: HazeState,
    startupAssistantUi: StartupAssistantUi,
    onPush: (NexusPage) -> Unit,
    onResetTo: (NexusPage) -> Unit,
) {
    val scope = rememberCoroutineScope()

    when (val page = entry.page) {
        StartupPage -> StartupPageContent(
            topPadding = topPadding,
            assistantUi = startupAssistantUi,
            onContinue = {
                onPush(
                    when (startupAssistantUi) {
                        StartupAssistantUi.Breeno,
                        StartupAssistantUi.XiaoAi -> ProviderPickPage
                        StartupAssistantUi.ChatOnly -> HomePage
                    }
                )
            },
        )

        ProviderPickPage -> {
            val deepSeekColors = providerButtonColors("deepseek")
            val openAiColors = providerButtonColors("openai")
            val claudeColors = providerButtonColors("anthropic")
            val geminiColors = providerButtonColors("google")

            SelectionPageContent(
                topPadding = topPadding,
                hazeState = hazeState,
                options = listOf(
                    SelectionOption(
                        id = "deepseek",
                        title = stringResource(R.string.ui_onboard_provider_pick_option_deepseek),
                        leadingIconRes = R.drawable.deepseek,
                        darkContainerColor = deepSeekColors.darkContainerColor,
                        lightContainerColor = deepSeekColors.lightContainerColor,
                        darkContentColor = deepSeekColors.darkContentColor,
                        lightContentColor = deepSeekColors.lightContentColor,
                        onClick = { onPush(ConfigurePage(providerId = "deepseek")) },
                    ),
                    SelectionOption(
                        id = "openai",
                        title = stringResource(R.string.ui_onboard_provider_pick_option_openai),
                        leadingIconRes = R.drawable.openai,
                        darkContainerColor = openAiColors.darkContainerColor,
                        lightContainerColor = openAiColors.lightContainerColor,
                        darkContentColor = openAiColors.darkContentColor,
                        lightContentColor = openAiColors.lightContentColor,
                        onClick = { onPush(ConfigurePage(providerId = "openai")) },
                    ),
                    SelectionOption(
                        id = "anthropic",
                        title = stringResource(R.string.ui_onboard_provider_pick_option_anthropic),
                        leadingIconRes = R.drawable.anthropic,
                        darkContainerColor = claudeColors.darkContainerColor,
                        lightContainerColor = claudeColors.lightContainerColor,
                        darkContentColor = claudeColors.darkContentColor,
                        lightContentColor = claudeColors.lightContentColor,
                        onClick = { onPush(ConfigurePage(providerId = "anthropic")) },
                    ),
                    SelectionOption(
                        id = "google",
                        title = stringResource(R.string.ui_onboard_provider_pick_option_google),
                        leadingIconRes = R.drawable.gemini,
                        tintLeadingIcon = false,
                        darkContainerColor = geminiColors.darkContainerColor,
                        lightContainerColor = geminiColors.lightContainerColor,
                        darkContentColor = geminiColors.darkContentColor,
                        lightContentColor = geminiColors.lightContentColor,
                        onClick = { onPush(ConfigurePage(providerId = "google")) },
                    ),
                ),
            )
        }

        is ConfigurePage -> {
            val colors = providerButtonColors(page.providerId)
            ConfigurePageContent(
                topPadding = topPadding,
                hazeState = hazeState,
                buttonDarkContainerColor = colors.darkContainerColor,
                buttonLightContainerColor = colors.lightContainerColor,
                buttonDarkContentColor = colors.darkContentColor,
                buttonLightContentColor = colors.lightContentColor,
                onComplete = { onPush(DonePage) },
            )
        }

        DonePage -> DonePageContent(
            topPadding = topPadding,
            hazeState = hazeState,
            onEnterHome = {
                scope.launch {
                    completeOnboarding()
                    onResetTo(HomePage)
                }
            },
        )

        HomePage -> HomePageContent(
            topPadding = topPadding,
            hazeState = hazeState,
        )

        SettingsHomePage -> SettingsHomePageContent(
            topPadding = topPadding,
            hazeState = hazeState,
            onOpenGroup = { group -> onPush(SettingsDetailPage(group)) },
        )

        is SettingsDetailPage -> SettingsDetailPageContent(
            group = page.group,
            topPadding = topPadding,
            hazeState = hazeState,
        )
    }
}

private suspend fun completeOnboarding() {
    val latestSettings = XService.getLocalSettings(context = com.niki914.nexus.h.util.ContextProvider.await())
    if (latestSettings.onboardingCompleted) {
        return
    }
    val updatedProps = latestSettings.props.toMutableMap().apply {
        this["onboarding_completed"] = JsonPrimitive(true)
    }
    XService.putLocalSettings(
        context = com.niki914.nexus.h.util.ContextProvider.await(),
        settings = LocalSettings(kotlinx.serialization.json.JsonObject(updatedProps)),
    )
}

private data class ProviderButtonColors(
    val darkContainerColor: Color,
    val lightContainerColor: Color,
    val darkContentColor: Color,
    val lightContentColor: Color,
)

@Composable
private fun providerButtonColors(providerId: String?): ProviderButtonColors {
    val black = colorResource(R.color.black)
    val white = colorResource(R.color.white)
    return when (providerId) {
        "deepseek" -> {
            val container = colorResource(R.color.provider_deepseek)
            ProviderButtonColors(
                darkContainerColor = container,
                lightContainerColor = container,
                darkContentColor = white,
                lightContentColor = white,
            )
        }

        "openai" -> ProviderButtonColors(
            darkContainerColor = colorResource(R.color.provider_openai_dark),
            lightContainerColor = colorResource(R.color.provider_openai_light),
            darkContentColor = black,
            lightContentColor = white,
        )

        "anthropic" -> {
            val container = colorResource(R.color.provider_claude)
            val content = colorResource(R.color.provider_claude_content)
            ProviderButtonColors(
                darkContainerColor = container,
                lightContainerColor = container,
                darkContentColor = content,
                lightContentColor = content,
            )
        }

        else -> ProviderButtonColors(
            darkContainerColor = MaterialTheme.colorScheme.primary,
            lightContainerColor = MaterialTheme.colorScheme.primary,
            darkContentColor = MaterialTheme.colorScheme.onPrimary,
            lightContentColor = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun DonePageContent(
    topPadding: Dp,
    hazeState: HazeState,
    onEnterHome: () -> Unit,
) {
    ConfigurePageContent(
        topPadding = topPadding,
        hazeState = hazeState,
        headline = stringResource(R.string.ui_onboard_done_headline),
        description = stringResource(R.string.ui_onboard_done_description),
        buttonText = stringResource(R.string.ui_onboard_done_enter_home),
        onComplete = onEnterHome,
    )
}
