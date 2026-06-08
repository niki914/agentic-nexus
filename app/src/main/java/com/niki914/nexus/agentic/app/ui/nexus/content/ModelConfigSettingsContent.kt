package com.niki914.nexus.agentic.app.ui.nexus.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.niki914.nexus.agentic.app.ui.infra.nav.pageViewModel
import com.niki914.nexus.agentic.app.ui.nexus.model.ConfigureEffect
import com.niki914.nexus.agentic.app.ui.nexus.model.ConfigureIntent
import com.niki914.nexus.agentic.app.ui.nexus.model.ConfigureScene
import com.niki914.nexus.agentic.app.ui.nexus.model.ConfigureViewModel
import com.niki914.nexus.agentic.app.ui.nexus.model.hasUnsavedChanges

@Composable
fun ModelConfigSettingsContent(
    onBack: () -> Unit,
) {
    val viewModel = pageViewModel<ConfigureViewModel>(
        key = "settings-configure",
    )
    val uiState by viewModel.uiStateFlow.collectAsState()
    var pendingFocusField by rememberSaveable {
        mutableStateOf<ConfigureEditableField?>(null)
    }

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

    EditableSettingsDetailChrome(
        isCreating = false,
        hasUnsavedChanges = {
            uiState.scene == ConfigureScene.Settings && uiState.hasUnsavedChanges
        },
        onDiscardChanges = onBack,
    ) {
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
    }
}
