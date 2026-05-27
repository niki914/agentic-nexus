package com.niki914.nexus.agentic.app.ui.nexus

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.niki914.nexus.agentic.app.R
import com.niki914.nexus.agentic.app.ui.infra.nav.NavigationEntry
import com.niki914.nexus.agentic.app.ui.infra.nav.pageViewModel
import com.niki914.nexus.agentic.app.ui.nexus.content.ConfigurePageContent
import com.niki914.nexus.agentic.app.ui.nexus.content.HomePageContent
import com.niki914.nexus.agentic.app.ui.nexus.content.McpServerDetailContent
import com.niki914.nexus.agentic.app.ui.nexus.content.SelectionOption
import com.niki914.nexus.agentic.app.ui.nexus.content.SelectionPageContent
import com.niki914.nexus.agentic.app.ui.nexus.content.SettingsDetailPageContent
import com.niki914.nexus.agentic.app.ui.nexus.content.SettingsHomePageContent
import com.niki914.nexus.agentic.app.ui.nexus.content.StartupPageContent
import com.niki914.nexus.agentic.app.ui.nexus.model.ConfigureEffect
import com.niki914.nexus.agentic.app.ui.nexus.model.ConfigureIntent
import com.niki914.nexus.agentic.app.ui.nexus.model.ConfigureViewModel
import com.niki914.nexus.agentic.app.ui.nexus.model.ProviderButtonTokens
import com.niki914.nexus.agentic.app.ui.nexus.model.ProviderSpec
import com.niki914.nexus.agentic.app.ui.nexus.model.ProviderSpecs
import com.niki914.nexus.agentic.app.ui.nexus.model.StartupAssistantUi
import com.niki914.nexus.agentic.app.ui.nexus.nav.ConfigurePage
import com.niki914.nexus.agentic.app.ui.nexus.nav.DonePage
import com.niki914.nexus.agentic.app.ui.nexus.nav.HomePage
import com.niki914.nexus.agentic.app.ui.nexus.nav.McpServerDetailPage
import com.niki914.nexus.agentic.app.ui.nexus.nav.NexusPage
import com.niki914.nexus.agentic.app.ui.nexus.nav.ProviderPickPage
import com.niki914.nexus.agentic.app.ui.nexus.nav.SettingsDetailPage
import com.niki914.nexus.agentic.app.ui.nexus.nav.SettingsHomePage
import com.niki914.nexus.agentic.app.ui.nexus.nav.StartupPage
import com.niki914.nexus.agentic.mod.LocalSettings
import com.niki914.nexus.agentic.mod.XService
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
            SelectionPageContent(
                topPadding = topPadding,
                hazeState = hazeState,
                options = ProviderSpecs.all.map { spec ->
                    val colors = providerButtonColors(spec)
                    SelectionOption(
                        id = spec.id,
                        title = spec.brandName,
                        leadingIconRes = spec.iconRes,
                        tintLeadingIcon = spec.tintIcon,
                        darkContainerColor = colors.darkContainerColor,
                        lightContainerColor = colors.lightContainerColor,
                        darkContentColor = colors.darkContentColor,
                        lightContentColor = colors.lightContentColor,
                        onClick = { onPush(ConfigurePage(providerId = spec.id)) },
                    )
                },
            )
        }

        is ConfigurePage -> {
            val context = LocalContext.current.applicationContext
            val factory = remember(context) { createConfigureViewModelFactory(context) }
            val viewModel = pageViewModel<ConfigureViewModel>(
                key = page.providerId,
                factory = factory,
            )
            val uiState by viewModel.uiStateFlow.collectAsState()
            val colors = providerButtonColors(uiState.providerSpec)

            LaunchedEffect(page.providerId) {
                viewModel.sendIntent(ConfigureIntent.Initialize(page.providerId))
            }
            LaunchedEffect(viewModel) {
                viewModel.uiEffect.collect { effect ->
                    if (effect is ConfigureEffect.SaveSucceeded) {
                        onPush(DonePage)
                    }
                }
            }

            ConfigurePageContent(
                topPadding = topPadding,
                hazeState = hazeState,
                uiState = uiState,
                buttonDarkContainerColor = colors.darkContainerColor,
                buttonLightContainerColor = colors.lightContainerColor,
                buttonDarkContentColor = colors.darkContentColor,
                buttonLightContentColor = colors.lightContentColor,
                onEndpointOverrideChange = { enabled ->
                    viewModel.sendIntent(ConfigureIntent.SetEndpointOverride(enabled))
                },
                onEndpointChange = { endpoint ->
                    viewModel.sendIntent(ConfigureIntent.UpdateEndpoint(endpoint))
                },
                onModelChange = { model ->
                    viewModel.sendIntent(ConfigureIntent.UpdateModel(model))
                },
                onApiKeyChange = { apiKey ->
                    viewModel.sendIntent(ConfigureIntent.UpdateApiKey(apiKey))
                },
                onToggleApiKeyVisibility = {
                    viewModel.sendIntent(ConfigureIntent.ToggleApiKeyVisibility)
                },
                onComplete = { viewModel.sendIntent(ConfigureIntent.Save) },
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
            onPush = onPush,
        )

        is McpServerDetailPage -> McpServerDetailContent(
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
private fun providerButtonColors(spec: ProviderSpec): ProviderButtonColors {
    return spec.visualTokens.button.toProviderButtonColors()
}

@Composable
private fun ProviderButtonTokens.toProviderButtonColors(): ProviderButtonColors {
    val colorScheme = MaterialTheme.colorScheme
    return ProviderButtonColors(
        darkContainerColor = darkContainerColorRes?.let { id -> colorResource(id) } ?: colorScheme.primary,
        lightContainerColor = lightContainerColorRes?.let { id -> colorResource(id) } ?: colorScheme.primary,
        darkContentColor = darkContentColorRes?.let { id -> colorResource(id) } ?: colorScheme.onPrimary,
        lightContentColor = lightContentColorRes?.let { id -> colorResource(id) } ?: colorScheme.onPrimary,
    )
}

private fun createConfigureViewModelFactory(
    context: Context,
): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass == ConfigureViewModel::class.java)
            return ConfigureViewModel(
                loadSettings = { XService.getLocalSettings(context) },
                saveSettings = { settings -> XService.putLocalSettings(context, settings) },
            ) as T
        }
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
