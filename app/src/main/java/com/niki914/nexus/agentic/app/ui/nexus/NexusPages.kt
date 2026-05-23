package com.niki914.nexus.agentic.app.ui.nexus

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.niki914.nexus.agentic.app.R
import com.niki914.nexus.agentic.app.ui.infra.nav.NavigationEntry
import com.niki914.nexus.agentic.app.ui.nexus.content.ConfigurePageContent
import com.niki914.nexus.agentic.app.ui.nexus.content.HomePageContent
import com.niki914.nexus.agentic.app.ui.nexus.content.SelectionOption
import com.niki914.nexus.agentic.app.ui.nexus.content.SelectionPageContent
import com.niki914.nexus.agentic.app.ui.nexus.content.SettingsDetailPageContent
import com.niki914.nexus.agentic.app.ui.nexus.content.SettingsHomePageContent
import com.niki914.nexus.agentic.app.ui.nexus.content.StartupPageContent
import com.niki914.nexus.agentic.app.ui.nexus.model.HomeChatController
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

@Composable
fun NexusPageContent(
    entry: NavigationEntry<NexusPage>,
    topPadding: Dp,
    hazeState: HazeState,
    startupAssistantUi: StartupAssistantUi,
    homeChatController: HomeChatController,
    onPush: (NexusPage) -> Unit,
) {
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

        ProviderPickPage -> SelectionPageContent(
            topPadding = topPadding,
            hazeState = hazeState,
            headline = stringResource(R.string.nexus_provider_pick_headline),
            description = stringResource(R.string.nexus_provider_pick_description),
            options = listOf(
                SelectionOption(
                    id = "deepseek",
                    title = stringResource(R.string.nexus_provider_pick_option_deepseek),
                    summary = stringResource(R.string.nexus_provider_pick_option_deepseek_summary),
                    onClick = { onPush(ConfigurePage) },
                ),
                SelectionOption(
                    id = "openai",
                    title = stringResource(R.string.nexus_provider_pick_option_openai),
                    summary = stringResource(R.string.nexus_provider_pick_option_openai_summary),
                    onClick = { onPush(ConfigurePage) },
                ),
                SelectionOption(
                    id = "anthropic",
                    title = stringResource(R.string.nexus_provider_pick_option_anthropic),
                    summary = stringResource(R.string.nexus_provider_pick_option_anthropic_summary),
                    onClick = { onPush(ConfigurePage) },
                ),
                SelectionOption(
                    id = "google",
                    title = stringResource(R.string.nexus_provider_pick_option_google),
                    summary = stringResource(R.string.nexus_provider_pick_option_google_summary),
                    onClick = { onPush(ConfigurePage) },
                ),
            ),
        )

        ConfigurePage -> ConfigurePageContent(
            topPadding = topPadding,
            hazeState = hazeState,
            onComplete = { onPush(DonePage) },
        )

        DonePage -> DonePageContent(
            topPadding = topPadding,
            hazeState = hazeState,
            onEnterHome = { onPush(HomePage) },
        )

        HomePage -> HomePageContent(
            topPadding = topPadding,
            hazeState = hazeState,
            chatController = homeChatController,
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

@Composable
private fun DonePageContent(
    topPadding: Dp,
    hazeState: HazeState,
    onEnterHome: () -> Unit,
) {
    ConfigurePageContent(
        topPadding = topPadding,
        hazeState = hazeState,
        headline = stringResource(R.string.nexus_done_headline),
        description = stringResource(R.string.nexus_done_description),
        buttonText = stringResource(R.string.nexus_done_enter_home),
        onComplete = onEnterHome,
    )
}
