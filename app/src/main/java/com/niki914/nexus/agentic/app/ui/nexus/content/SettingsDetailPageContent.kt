package com.niki914.nexus.agentic.app.ui.nexus.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.niki914.nexus.agentic.app.R
import com.niki914.nexus.agentic.app.ui.infra.ConfirmationLiquidDialog
import com.niki914.nexus.agentic.app.ui.infra.nav.pageViewModel
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
import com.niki914.nexus.agentic.app.ui.nexus.nav.McpServerDetailPage
import com.niki914.nexus.agentic.app.ui.nexus.nav.NexusPage
import com.niki914.nexus.agentic.app.ui.nexus.nav.NexusSettingsGroup
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@Composable
fun SettingsDetailPageContent(
    group: NexusSettingsGroup,
    topPadding: Dp,
    hazeState: HazeState,
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
            topPadding = topPadding,
            hazeState = hazeState,
            onBack = onBack,
        )
        return
    }

    if (group == NexusSettingsGroup.BuiltinTools) {
        BuiltinToolsSettingsContent(
            topPadding = topPadding,
            hazeState = hazeState,
        )
        return
    }

    if (group == NexusSettingsGroup.CustomShellTools) {
        CustomShellToolsSettingsContent(
            topPadding = topPadding,
            hazeState = hazeState,
            onOpenToolDetail = { name, index ->
                onPush(CustomToolDetailPage(name, index))
            },
        )
        return
    }

    if (group == NexusSettingsGroup.Mcp) {
        McpSettingsContent(
            topPadding = topPadding,
            hazeState = hazeState,
            onOpenServerDetail = { name, index ->
                onPush(McpServerDetailPage(name, index))
            },
        )
        return
    }

    if (group == NexusSettingsGroup.Memory || group == NexusSettingsGroup.ExecutionRules) {
        TODOPageContent(
            topPadding = topPadding,
            hazeState = hazeState,
        )
        return
    }

    TODOPageContent(
        topPadding = topPadding,
        hazeState = hazeState,
    )
    return
}

@Composable
private fun ModelConfigSettingsContent(
    topPadding: Dp,
    hazeState: HazeState,
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

    val requestBack = remember {
        {
            if (latestUiState.scene == ConfigureScene.Settings && latestUiState.hasUnsavedChanges) {
                showUnsavedChangesDialog = true
            } else {
                latestOnBack()
            }
        }
    }
    RegisterPageChrome(
        PageChromeContribution(
            onBackRequest = requestBack,
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
        topPadding = topPadding,
        hazeState = hazeState,
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
