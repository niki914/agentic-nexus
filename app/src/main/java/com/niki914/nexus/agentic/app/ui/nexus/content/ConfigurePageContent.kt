package com.niki914.nexus.agentic.app.ui.nexus.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.niki914.nexus.agentic.app.R
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsDetailFormScaffold
import com.niki914.nexus.agentic.app.ui.infra.component.TintLiquidButton
import com.niki914.nexus.agentic.app.ui.nexus.model.ConfigureInlineError
import com.niki914.nexus.agentic.app.ui.nexus.model.ConfigureScene
import com.niki914.nexus.agentic.app.ui.nexus.model.ConfigureUiState
import com.niki914.nexus.agentic.app.ui.nexus.model.ProviderSpecs
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

@Composable
fun ConfigurePageContent(
    topPadding: Dp,
    hazeState: HazeState,
    uiState: ConfigureUiState,
    buttonDarkContainerColor: Color = MaterialTheme.colorScheme.primary,
    buttonLightContainerColor: Color = MaterialTheme.colorScheme.primary,
    buttonDarkContentColor: Color = MaterialTheme.colorScheme.onPrimary,
    buttonLightContentColor: Color = MaterialTheme.colorScheme.onPrimary,
    onEndpointOverrideChange: (Boolean) -> Unit,
    onEndpointChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onPromptChange: (String) -> Unit = {},
    onProxyChange: (String) -> Unit = {},
    onToggleApiKeyVisibility: () -> Unit,
    onComplete: () -> Unit,
    requestedFocusField: ConfigureEditableField? = null,
    onRequestedFocusHandled: () -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    val policy = configurePagePolicy(uiState.scene, uiState.providerSpec)
    val actionText = stringResource(
        when (uiState.scene) {
            ConfigureScene.Onboarding -> R.string.ui_onboard_configure_next
            ConfigureScene.Settings -> R.string.ui_settings_configure_save
        },
    )
    val description = stringResource(
        when (uiState.scene) {
            ConfigureScene.Onboarding -> R.string.ui_onboard_configure_description
            ConfigureScene.Settings -> R.string.ui_settings_configure_description
        },
    )
    var expandedField by rememberSaveable { mutableStateOf<ConfigureEditableField?>(null) }

    fun clearActiveField() {
        expandedField = null
        focusManager.clearFocus()
    }

    LaunchedEffect(uiState.endpointOverrideEnabled) {
        if (!uiState.endpointOverrideEnabled && expandedField == ConfigureEditableField.Endpoint) {
            expandedField = null
        }
    }

    LaunchedEffect(requestedFocusField) {
        if (requestedFocusField != null) {
            expandedField = requestedFocusField
            onRequestedFocusHandled()
        }
    }

    SettingsDetailFormScaffold(
        topPadding = topPadding,
        hazeState = hazeState,
        actionText = actionText,
        onActionClick = onComplete,
        description = description,
        inlineErrorText = configureInlineErrorText(uiState.inlineError),
        actionEnabled = !uiState.isSaving,
        onBackgroundTap = ::clearActiveField,
        actionButtonDarkContainerColor = buttonDarkContainerColor,
        actionButtonLightContainerColor = buttonLightContainerColor,
        actionButtonDarkContentColor = buttonDarkContentColor,
        actionButtonLightContentColor = buttonLightContentColor,
    ) {
        ProviderAccessSettingsBlock(
            uiState = uiState,
            policy = policy,
            expandedField = expandedField,
            onExpandedFieldChange = { field -> expandedField = field },
            onEndpointOverrideChange = onEndpointOverrideChange,
            onEndpointChange = onEndpointChange,
            onModelChange = onModelChange,
            onApiKeyChange = onApiKeyChange,
            onToggleApiKeyVisibility = onToggleApiKeyVisibility,
            onClearActiveField = ::clearActiveField,
        )
        if (policy.showAdvancedSection) {
            ProviderAdvancedSettingsBlock(
                uiState = uiState,
                expandedField = expandedField,
                onExpandedFieldChange = { field -> expandedField = field },
                onPromptChange = onPromptChange,
                onProxyChange = onProxyChange,
            )
        }
    }
}

@Composable
internal fun ConfigurePageContent(
    topPadding: Dp,
    hazeState: HazeState,
    headline: String,
    description: String,
    buttonText: String,
    buttonDarkContainerColor: Color = MaterialTheme.colorScheme.primary,
    buttonLightContainerColor: Color = MaterialTheme.colorScheme.primary,
    buttonDarkContentColor: Color = MaterialTheme.colorScheme.onPrimary,
    buttonLightContentColor: Color = MaterialTheme.colorScheme.onPrimary,
    onComplete: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .hazeSource(hazeState)
            .padding(top = topPadding)
            .padding(horizontal = 24.dp, vertical = 24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = headline,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            TintLiquidButton(
                text = buttonText,
                darkContainerColor = buttonDarkContainerColor,
                lightContainerColor = buttonLightContainerColor,
                darkContentColor = buttonDarkContentColor,
                lightContentColor = buttonLightContentColor,
                onClick = onComplete,
            )
        }
    }
}

@Composable
private fun configureInlineErrorText(error: ConfigureInlineError?): String? {
    return when (error) {
        null -> null
        is ConfigureInlineError.LoadFailed -> stringResource(
            R.string.ui_onboard_configure_error_load_failed,
            error.reason.message,
        )
        is ConfigureInlineError.SaveFailed -> stringResource(
            R.string.ui_onboard_configure_error_save_failed,
            error.reason.message,
        )
    }
}

@Preview(
    name = "Configure Page Preview",
    showBackground = true,
    widthDp = 420,
    heightDp = 900,
)
@Composable
private fun ConfigurePageContentPreview() {
    MaterialTheme {
        val hazeState = rememberHazeState(blurEnabled = true)
        ConfigurePageContent(
            topPadding = 0.dp,
            hazeState = hazeState,
            uiState = ConfigureUiState(
                providerSpec = ProviderSpecs.find("deepseek"),
                endpointOverrideEnabled = false,
                endpointInput = ProviderSpecs.find("deepseek").officialEndpoint,
                modelInput = "deepseek-chat",
                apiKeyInput = "sk-demo-key",
                apiKeyVisible = false,
                saveEnabled = true,
            ),
            onEndpointOverrideChange = {},
            onEndpointChange = {},
            onModelChange = {},
            onApiKeyChange = {},
            onToggleApiKeyVisibility = {},
            onComplete = {},
        )
    }
}
