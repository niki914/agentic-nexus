package com.niki914.nexus.agentic.app.ui.nexus.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.niki914.nexus.agentic.app.R
import com.niki914.nexus.agentic.app.ui.infra.ConfirmationLiquidDialog
import com.niki914.nexus.agentic.app.ui.infra.nav.pageViewModel
import com.niki914.nexus.agentic.app.ui.nexus.PageBackHandler
import com.niki914.nexus.agentic.app.ui.nexus.PageChromeContribution
import com.niki914.nexus.agentic.app.ui.nexus.RegisterPageChrome
import com.niki914.nexus.agentic.app.ui.nexus.content.mcp.McpSettingsContent
import com.niki914.nexus.agentic.app.ui.nexus.model.ConfigureEffect
import com.niki914.nexus.agentic.app.ui.nexus.model.ConfigureIntent
import com.niki914.nexus.agentic.app.ui.nexus.model.ConfigureScene
import com.niki914.nexus.agentic.app.ui.nexus.model.ConfigureViewModel
import com.niki914.nexus.agentic.app.ui.nexus.model.SettingsViewModel
import com.niki914.nexus.agentic.app.ui.nexus.model.hasUnsavedChanges
import com.niki914.nexus.agentic.app.ui.nexus.nav.CustomToolDetailPage
import com.niki914.nexus.agentic.app.ui.nexus.nav.ExecutionRuleDetailPage
import com.niki914.nexus.agentic.app.ui.nexus.nav.McpServerDetailPage
import com.niki914.nexus.agentic.app.ui.nexus.nav.NexusPage
import com.niki914.nexus.agentic.app.ui.nexus.nav.NexusSettingsGroup

@Composable
fun SettingsDetailPageContent(
    group: NexusSettingsGroup,
    onPush: (NexusPage) -> Unit,
    onBack: () -> Unit,
) {
    val viewModel = pageViewModel<SettingsViewModel>()
    val uiState by viewModel.uiStateFlow.collectAsState()
    val visibleGroups = uiState.sections.flatMap { it.groups }.toSet()
    if (group !in visibleGroups) {
        return
    }

    if (group == NexusSettingsGroup.ModelConfig) {
        ModelConfigSettingsContent(
            onBack = onBack,
        )
        return
    }

    if (group == NexusSettingsGroup.BuiltinTools) {
        BuiltinToolsSettingsContent()
        return
    }

    if (group == NexusSettingsGroup.CustomShellTools) {
        CustomShellToolsSettingsContent(
            onOpenToolDetail = { name, index ->
                onPush(CustomToolDetailPage(name, index))
            },
        )
        return
    }

    if (group == NexusSettingsGroup.Mcp) {
        McpSettingsContent(
            onOpenServerDetail = { name, index ->
                onPush(McpServerDetailPage(name, index))
            },
        )
        return
    }

    if (group == NexusSettingsGroup.About) {
        AboutSettingsContent()
        return
    }

    if (group == NexusSettingsGroup.Memory) {
        MemorySettingsContent()
        return
    }

    if (group == NexusSettingsGroup.ExecutionRules) {
        ExecutionRulesSettingsContent(
            onOpenRuleDetail = { name, index ->
                onPush(ExecutionRuleDetailPage(name, index))
            },
        )
        return
    }

    TODOPageContent()
    return
}

@Composable
private fun ModelConfigSettingsContent(
    onBack: () -> Unit,
) {
    val viewModel = pageViewModel<ConfigureViewModel>(
        key = "settings-configure",
    )
    val uiState by viewModel.uiStateFlow.collectAsState()
    val latestUiState by rememberUpdatedState(uiState)
    val latestOnBack by rememberUpdatedState(onBack)
    var pendingFocusField by rememberSaveable {
        mutableStateOf<ConfigureEditableField?>(null)
    }
    var showUnsavedChangesDialog by rememberSaveable { mutableStateOf(false) }

    RegisterPageChrome(
        PageChromeContribution(
            backHandler = PageBackHandler(
                shouldConsumeBack = {
                    latestUiState.scene == ConfigureScene.Settings && latestUiState.hasUnsavedChanges
                },
                onConsumeBack = {
                    showUnsavedChangesDialog = true
                },
            ),
        ),
    )

    LaunchedEffect(viewModel) {
        viewModel.sendIntent(ConfigureIntent.Initialize(scene = ConfigureScene.Settings))
    }
    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                ConfigureEffect.SettingsSaveSucceeded -> onBack()
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

                ConfigureEffect.OnboardingSaveSucceeded,
                is ConfigureEffect.SaveFailed -> Unit
            }
        }
    }

    ConfigurePageContent(
        uiState = uiState,
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
        onPromptChange = { prompt ->
            viewModel.sendIntent(ConfigureIntent.UpdatePrompt(prompt))
        },
        onProxyChange = { proxy ->
            viewModel.sendIntent(ConfigureIntent.UpdateProxy(proxy))
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

    ConfirmationLiquidDialog(
        visible = showUnsavedChangesDialog,
        onDismissRequest = {
            showUnsavedChangesDialog = false
        },
        title = stringResource(R.string.unsaved_changes_dialog_title),
        text = stringResource(R.string.unsaved_changes_dialog_text),
        negativeButtonText = stringResource(R.string.unsaved_changes_dialog_cancel),
        positiveButtonText = stringResource(R.string.unsaved_changes_dialog_confirm_exit),
        onNegativeClick = {
            showUnsavedChangesDialog = false
        },
        onPositiveClick = {
            showUnsavedChangesDialog = false
            latestOnBack()
        },
    )
}
