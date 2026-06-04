package com.niki914.nexus.agentic.app.ui.nexus

import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import com.niki914.nexus.agentic.app.R
import com.niki914.nexus.agentic.app.ui.infra.ConfirmationLiquidDialog
import com.niki914.nexus.agentic.app.ui.infra.nav.NavigationEntry
import com.niki914.nexus.agentic.app.ui.infra.nav.pageViewModel
import com.niki914.nexus.agentic.app.ui.nexus.content.ConfigureEditableField
import com.niki914.nexus.agentic.app.ui.nexus.content.ConfigurePageContent
import com.niki914.nexus.agentic.app.ui.nexus.content.CustomToolDetailContent
import com.niki914.nexus.agentic.app.ui.nexus.content.DonePageContent
import com.niki914.nexus.agentic.app.ui.nexus.content.HomePageContent
import com.niki914.nexus.agentic.app.ui.nexus.content.SelectionOption
import com.niki914.nexus.agentic.app.ui.nexus.content.SelectionPageContent
import com.niki914.nexus.agentic.app.ui.nexus.content.SettingsDetailPageContent
import com.niki914.nexus.agentic.app.ui.nexus.content.SettingsHomePageContent
import com.niki914.nexus.agentic.app.ui.nexus.content.StartupPageContent
import com.niki914.nexus.agentic.app.ui.nexus.content.mcp.McpServerDetailContent
import com.niki914.nexus.agentic.app.ui.nexus.model.ConfigureEffect
import com.niki914.nexus.agentic.app.ui.nexus.model.ConfigureIntent
import com.niki914.nexus.agentic.app.ui.nexus.model.ConfigureViewModel
import com.niki914.nexus.agentic.app.ui.nexus.model.ProviderButtonTokens
import com.niki914.nexus.agentic.app.ui.nexus.model.ProviderSpec
import com.niki914.nexus.agentic.app.ui.nexus.model.ProviderSpecs
import com.niki914.nexus.agentic.app.ui.nexus.model.StartupAssistantUi
import com.niki914.nexus.agentic.app.ui.nexus.nav.ConfigurePage
import com.niki914.nexus.agentic.app.ui.nexus.nav.CustomToolDetailPage
import com.niki914.nexus.agentic.app.ui.nexus.nav.DonePage
import com.niki914.nexus.agentic.app.ui.nexus.nav.HomePage
import com.niki914.nexus.agentic.app.ui.nexus.nav.McpServerDetailPage
import com.niki914.nexus.agentic.app.ui.nexus.nav.NexusPage
import com.niki914.nexus.agentic.app.ui.nexus.nav.NexusSettingsGroup
import com.niki914.nexus.agentic.app.ui.nexus.nav.PageTitleSpec
import com.niki914.nexus.agentic.app.ui.nexus.nav.ProviderPickPage
import com.niki914.nexus.agentic.app.ui.nexus.nav.ResTitle
import com.niki914.nexus.agentic.app.ui.nexus.nav.SettingsDetailPage
import com.niki914.nexus.agentic.app.ui.nexus.nav.SettingsHomePage
import com.niki914.nexus.agentic.app.ui.nexus.nav.StartupPage
import com.niki914.nexus.agentic.app.ui.nexus.nav.TextTitle
import com.niki914.nexus.agentic.app.ui.nexus.nav.TopBarActionSpec
import com.niki914.nexus.agentic.repo.XRepo
import com.niki914.nexus.agentic.repo.WebSettingsFailureReason
import com.niki914.nexus.agentic.repo.WebSettingsResult
import androidx.compose.material.icons.Icons
import kotlinx.coroutines.launch

@Composable
fun NexusPageContent(
    entry: NavigationEntry<NexusPage>,
    startupAssistantUi: StartupAssistantUi,
    onPush: (NexusPage) -> Unit,
    onPop: () -> Unit,
    onResetTo: (NexusPage) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val mcpCreateTitle = stringResource(R.string.mcp_editor_title_create)
    val customToolCreateTitle = stringResource(R.string.custom_tool_editor_title_create)

    when (val page = entry.page) {
        StartupPage -> {
            var isCheckingWebSettings by rememberSaveable { mutableStateOf(false) }
            var webSettingsDialog by rememberSaveable {
                mutableStateOf<StartupWebSettingsDialog?>(null)
            }

            fun nextPage(): NexusPage {
                return when (startupAssistantUi) {
                    StartupAssistantUi.Breeno,
                    StartupAssistantUi.XiaoAi -> ProviderPickPage

                    StartupAssistantUi.ChatOnly -> HomePage
                }
            }

            fun enterNextPage() {
                webSettingsDialog = null
                onPush(nextPage())
            }

            fun handleWebSettingsResult(result: WebSettingsResult) {
                isCheckingWebSettings = false
                when (result) {
                    is WebSettingsResult.Success -> {
                        webSettingsDialog = when {
                            result.isFallbackVersion -> StartupWebSettingsDialog.UnsupportedVersion
                            result.settings.isBeta -> StartupWebSettingsDialog.Beta
                            else -> null
                        }
                        if (webSettingsDialog == null) {
                            enterNextPage()
                        }
                    }

                    is WebSettingsResult.RequestFailed -> {
                        webSettingsDialog = when (result.reason) {
                            WebSettingsFailureReason.NetworkUnavailable -> StartupWebSettingsDialog.NetworkError
                            WebSettingsFailureReason.ServerError,
                            WebSettingsFailureReason.InvalidConfig -> StartupWebSettingsDialog.FetchFailed

                            WebSettingsFailureReason.UnsupportedVersion -> {
                                StartupWebSettingsDialog.UnsupportedVersion
                            }
                        }
                    }
                }
            }

            fun requestWebSettings(forceRetry: Boolean) {
                scope.launch {
                    isCheckingWebSettings = true
                    webSettingsDialog = null
                    val result = if (forceRetry) {
                        XRepo.web.retry()
                    } else {
                        XRepo.web.await()
                    }
                    handleWebSettingsResult(result)
                }
            }

            StartupPageContent(
                assistantUi = startupAssistantUi,
                isLoading = isCheckingWebSettings,
                onContinue = {
                    if (startupAssistantUi == StartupAssistantUi.ChatOnly) {
                        enterNextPage()
                    } else {
                        requestWebSettings(forceRetry = false)
                    }
                },
            )

            webSettingsDialog?.let { dialog ->
                StartupWebSettingsDialogContent(
                    dialog = dialog,
                    onEnterNextPage = ::enterNextPage,
                    onRetry = {
                        requestWebSettings(forceRetry = true)
                    },
                    onDismiss = {
                        webSettingsDialog = null
                    },
                )
            }
        }

        ProviderPickPage -> {
            SelectionPageContent(
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
                        onClick = {
                            onPush(
                                ConfigurePage(
                                    providerId = spec.id,
                                    explicitTitleSpec = TextTitle(spec.brandName),
                                ),
                            )
                        },
                    )
                },
            )
        }

        is ConfigurePage -> {
            val viewModel = pageViewModel<ConfigureViewModel>(
                key = page.providerId,
            )
            val uiState by viewModel.uiStateFlow.collectAsState()
            val colors = providerButtonColors(uiState.providerSpec)
            var pendingFocusField by rememberSaveable {
                mutableStateOf<ConfigureEditableField?>(null)
            }

            LaunchedEffect(page.providerId) {
                viewModel.sendIntent(ConfigureIntent.Initialize(page.providerId))
            }
            LaunchedEffect(viewModel) {
                viewModel.uiEffect.collect { effect ->
                    when (effect) {
                        ConfigureEffect.OnboardingSaveSucceeded -> onPush(DonePage)
                        ConfigureEffect.SettingsSaveSucceeded -> Unit
                        ConfigureEffect.FocusModel -> {
                            pendingFocusField = ConfigureEditableField.Model
                        }

                        ConfigureEffect.FocusApiKey -> {
                            pendingFocusField = ConfigureEditableField.ApiKey
                        }

                        ConfigureEffect.FocusEndpoint -> {
                            pendingFocusField = ConfigureEditableField.Endpoint
                        }

                        ConfigureEffect.FocusProxy -> {
                            pendingFocusField = ConfigureEditableField.Proxy
                        }

                        is ConfigureEffect.SaveFailed -> Unit
                    }
                }
            }

            ConfigurePageContent(
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
                requestedFocusField = pendingFocusField,
                onRequestedFocusHandled = {
                    pendingFocusField = null
                },
            )
        }

        DonePage -> DonePageContent(
            onEnterHome = {
                scope.launch {
                    completeOnboarding()
                    onResetTo(HomePage)
                }
            },
        )

        HomePage -> HomePageContent(
            onOpenSettings = {
                onPush(SettingsHomePage)
            },
        )

        SettingsHomePage -> SettingsHomePageContent(
            onOpenGroup = { group ->
                onPush(
                    SettingsDetailPage(
                        group = group,
                        explicitTitleSpec = settingsDetailTitleSpec(group),
                        explicitRightAction = settingsDetailRightAction(
                            group = group,
                            mcpCreateTitle = mcpCreateTitle,
                            customToolCreateTitle = customToolCreateTitle,
                            onPush = onPush,
                        ),
                    )
                )
            },
        )

        is SettingsDetailPage -> SettingsDetailPageContent(
            group = page.group,
            onPush = onPush,
            onBack = onPop,
        )

        is McpServerDetailPage -> McpServerDetailContent(
            page = page,
            onBack = onPop,
        )

        is CustomToolDetailPage -> CustomToolDetailContent(
            page = page,
            onBack = onPop,
        )
    }
}

@Composable
private fun StartupWebSettingsDialogContent(
    dialog: StartupWebSettingsDialog,
    onEnterNextPage: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val titleRes = when (dialog) {
        StartupWebSettingsDialog.Beta -> R.string.ui_onboard_web_beta_title
        StartupWebSettingsDialog.FetchFailed -> R.string.ui_onboard_web_fetch_failed_title
        StartupWebSettingsDialog.UnsupportedVersion -> R.string.ui_onboard_web_unsupported_title
        StartupWebSettingsDialog.NetworkError -> R.string.ui_onboard_web_network_error_title
    }
    val bodyRes = when (dialog) {
        StartupWebSettingsDialog.Beta -> R.string.ui_onboard_web_beta_body
        StartupWebSettingsDialog.FetchFailed -> R.string.ui_onboard_web_fetch_failed_body
        StartupWebSettingsDialog.UnsupportedVersion -> R.string.ui_onboard_web_unsupported_body
        StartupWebSettingsDialog.NetworkError -> R.string.ui_onboard_web_network_error_body
    }
    val positiveTextRes = when (dialog) {
        StartupWebSettingsDialog.Beta,
        StartupWebSettingsDialog.UnsupportedVersion -> R.string.ui_onboard_web_confirm

        StartupWebSettingsDialog.FetchFailed -> R.string.ui_onboard_web_enter_directly
        StartupWebSettingsDialog.NetworkError -> R.string.ui_onboard_web_retry
    }
    val negativeTextRes = when (dialog) {
        StartupWebSettingsDialog.Beta,
        StartupWebSettingsDialog.UnsupportedVersion,
        StartupWebSettingsDialog.NetworkError -> R.string.ui_onboard_web_cancel

        StartupWebSettingsDialog.FetchFailed -> R.string.ui_onboard_web_retry
    }
    val onPositiveClick = when (dialog) {
        StartupWebSettingsDialog.Beta,
        StartupWebSettingsDialog.UnsupportedVersion,
        StartupWebSettingsDialog.FetchFailed -> onEnterNextPage

        StartupWebSettingsDialog.NetworkError -> onRetry
    }
    val onNegativeClick = when (dialog) {
        StartupWebSettingsDialog.Beta,
        StartupWebSettingsDialog.UnsupportedVersion,
        StartupWebSettingsDialog.NetworkError -> onDismiss

        StartupWebSettingsDialog.FetchFailed -> onRetry
    }

    ConfirmationLiquidDialog(
        visible = true,
        onDismissRequest = onDismiss,
        title = stringResource(titleRes),
        text = stringResource(bodyRes),
        positiveButtonText = stringResource(positiveTextRes),
        negativeButtonText = stringResource(negativeTextRes),
        onPositiveClick = onPositiveClick,
        onNegativeClick = onNegativeClick,
        dismissOnBackgroundTap = false,
    )
}

private enum class StartupWebSettingsDialog {
    Beta,
    FetchFailed,
    UnsupportedVersion,
    NetworkError,
}

private fun settingsDetailTitleSpec(group: NexusSettingsGroup): PageTitleSpec? {
    return when (group) {
        NexusSettingsGroup.Mcp -> ResTitle(R.string.ui_settings_mcp_config)
        else -> null
    }
}

private fun settingsDetailRightAction(
    group: NexusSettingsGroup,
    mcpCreateTitle: String,
    customToolCreateTitle: String,
    onPush: (NexusPage) -> Unit,
): TopBarActionSpec? {
    return when (group) {
        NexusSettingsGroup.Mcp -> TopBarActionSpec(
            icon = Icons.Default.Add,
            onClick = {
                onPush(
                    McpServerDetailPage(
                        serverName = mcpCreateTitle,
                        serverIndex = -1,
                        isCreating = true,
                    )
                )
            },
        )

        NexusSettingsGroup.CustomShellTools -> TopBarActionSpec(
            icon = Icons.Default.Add,
            onClick = {
                onPush(
                    CustomToolDetailPage(
                        toolName = customToolCreateTitle,
                        toolIndex = -1,
                        isCreating = true,
                    )
                )
            },
        )

        else -> null
    }
}

private suspend fun completeOnboarding() {
    if (XRepo.onboardingCompleted()) {
        return
    }
    XRepo.setOnboardingCompleted(true)
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
        darkContainerColor = darkContainerColorRes?.let { id -> colorResource(id) }
            ?: colorScheme.primary,
        lightContainerColor = lightContainerColorRes?.let { id -> colorResource(id) }
            ?: colorScheme.primary,
        darkContentColor = darkContentColorRes?.let { id -> colorResource(id) }
            ?: colorScheme.onPrimary,
        lightContentColor = lightContentColorRes?.let { id -> colorResource(id) }
            ?: colorScheme.onPrimary,
    )
}
