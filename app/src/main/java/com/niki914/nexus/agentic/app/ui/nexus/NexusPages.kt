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
import com.niki914.nexus.agentic.app.ui.nexus.nav.BrandCheckPage
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
    onPush: (NexusPage) -> Unit,
) {
    when (val page = entry.page) {
        StartupPage -> StartupPageContent(
            topPadding = topPadding,
            hazeState = hazeState,
            onContinue = { onPush(BrandCheckPage) },
        )

        BrandCheckPage -> SelectionPageContent(
            topPadding = topPadding,
            hazeState = hazeState,
            headline = stringResource(R.string.nexus_brand_check_headline),
            description = stringResource(R.string.nexus_brand_check_description),
            options = listOf(
                SelectionOption(
                    id = "breeno",
                    title = stringResource(R.string.nexus_brand_check_option_breeno),
                    summary = stringResource(R.string.nexus_brand_check_option_breeno_summary),
                    onClick = { onPush(ProviderPickPage) },
                ),
                SelectionOption(
                    id = "xiaoai",
                    title = stringResource(R.string.nexus_brand_check_option_xiaoai),
                    summary = stringResource(R.string.nexus_brand_check_option_xiaoai_summary),
                    onClick = { onPush(ProviderPickPage) },
                ),
            ),
        )

        ProviderPickPage -> SelectionPageContent(
            topPadding = topPadding,
            hazeState = hazeState,
            headline = stringResource(R.string.nexus_provider_pick_headline),
            description = stringResource(R.string.nexus_provider_pick_description),
            options = listOf(
                SelectionOption(
                    id = "openai-compatible",
                    title = stringResource(R.string.nexus_provider_pick_option_openai),
                    summary = stringResource(R.string.nexus_provider_pick_option_openai_summary),
                    onClick = { onPush(ConfigurePage) },
                ),
                SelectionOption(
                    id = "anthropic-compatible",
                    title = stringResource(R.string.nexus_provider_pick_option_anthropic),
                    summary = stringResource(R.string.nexus_provider_pick_option_anthropic_summary),
                    onClick = { onPush(ConfigurePage) },
                ),
                SelectionOption(
                    id = "custom",
                    title = stringResource(R.string.nexus_provider_pick_option_custom),
                    summary = stringResource(R.string.nexus_provider_pick_option_custom_summary),
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
