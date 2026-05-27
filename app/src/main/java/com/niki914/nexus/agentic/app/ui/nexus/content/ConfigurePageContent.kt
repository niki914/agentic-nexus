package com.niki914.nexus.agentic.app.ui.nexus.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niki914.nexus.agentic.app.R
import com.niki914.nexus.agentic.app.ui.infra.component.SettingExpandableTextItem
import com.niki914.nexus.agentic.app.ui.infra.component.SettingToggleItem
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsGroupCard
import com.niki914.nexus.agentic.app.ui.infra.component.TintLiquidButton
import com.niki914.nexus.agentic.app.ui.nexus.model.ConfigureInlineError
import com.niki914.nexus.agentic.app.ui.nexus.model.ConfigureUiState
import com.niki914.nexus.agentic.app.ui.nexus.model.ProviderSpecs
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

// TODO P1 ViewModel
// TODO P0 改描述文案和自定义标题“DeepSeek”之类的。并且根据选择的供应商可决定是否将自定义 Endpoint 和 Endpoint 干掉，像 DeepSeek 这种就不需要展示
// TODO P0 文案 hint 重设计
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
    onToggleApiKeyVisibility: () -> Unit,
    onComplete: () -> Unit,
) {
    val scrollState = rememberScrollState()
    var expandedField by rememberSaveable { mutableStateOf<ConfigureExpandableField?>(null) }

    LaunchedEffect(uiState.endpointOverrideEnabled) {
        if (!uiState.endpointOverrideEnabled && expandedField == ConfigureExpandableField.Endpoint) {
            expandedField = null
        }
    }

    fun updateExpandedField(field: ConfigureExpandableField, expanded: Boolean) {
        expandedField = if (expanded) {
            field
        } else if (expandedField == field) {
            null
        } else {
            expandedField
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .hazeSource(hazeState)
            .padding(top = topPadding)
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text(
                    text = stringResource(
                        R.string.ui_onboard_configure_description,
                        uiState.providerSpec.brandName,
                    ),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        lineHeight = 28.sp,
                    ),
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                SettingsGroupCard(title = uiState.providerSpec.brandName) {
                    SettingToggleItem(
                        title = stringResource(R.string.ui_onboard_configure_endpoint_override_title),
                        description = stringResource(R.string.ui_onboard_configure_endpoint_override_description),
                        checked = uiState.endpointOverrideEnabled,
                        enabled = !uiState.isSaving,
                        onCheckedChange = { enabled ->
                            if (!uiState.isSaving) {
                                onEndpointOverrideChange(enabled)
                            }
                        },
                    )
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    SettingExpandableTextItem(
                        title = stringResource(R.string.ui_onboard_configure_endpoint_label),
                        value = uiState.endpointInput,
                        onValueChange = onEndpointChange,
                        placeholder = stringResource(R.string.ui_onboard_configure_endpoint_placeholder),
                        description = uiState.providerSpec.officialEndpoint,
                        enabled = uiState.endpointOverrideEnabled && !uiState.isSaving,
                        minLines = 3,
                        maxLines = 6,
                        expanded = expandedField == ConfigureExpandableField.Endpoint,
                        onExpandedChange = { isExpanded ->
                            updateExpandedField(ConfigureExpandableField.Endpoint, isExpanded)
                        },
                    )
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    SettingExpandableTextItem(
                        title = stringResource(R.string.ui_onboard_configure_model_label),
                        value = uiState.modelInput,
                        onValueChange = onModelChange,
                        placeholder = stringResource(R.string.ui_onboard_configure_model_placeholder),
                        description = stringResource(R.string.ui_onboard_configure_model_placeholder),
                        enabled = !uiState.isSaving,
                        minLines = 1,
                        maxLines = 1,
                        expanded = expandedField == ConfigureExpandableField.Model,
                        onExpandedChange = { isExpanded ->
                            updateExpandedField(ConfigureExpandableField.Model, isExpanded)
                        },
                    )
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    SettingExpandableTextItem(
                        title = stringResource(R.string.ui_onboard_configure_api_key_label),
                        value = uiState.apiKeyInput,
                        onValueChange = onApiKeyChange,
                        placeholder = stringResource(R.string.ui_onboard_configure_api_key_placeholder),
                        description = stringResource(R.string.ui_onboard_configure_api_key_placeholder),
                        enabled = !uiState.isSaving,
                        minLines = 1,
                        maxLines = 1,
                        secretVisible = uiState.apiKeyVisible,
                        onToggleSecretVisibility = onToggleApiKeyVisibility,
                        toggleSecretVisibleContentDescription = stringResource(
                            R.string.ui_onboard_configure_api_key_show,
                        ),
                        toggleSecretHiddenContentDescription = stringResource(
                            R.string.ui_onboard_configure_api_key_hide,
                        ),
                        expanded = expandedField == ConfigureExpandableField.ApiKey,
                        onExpandedChange = { isExpanded ->
                            updateExpandedField(ConfigureExpandableField.ApiKey, isExpanded)
                        },
                    )
                    configureInlineErrorText(uiState.inlineError)?.let { errorText ->
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                        Text(
                            text = errorText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            TintLiquidButton(
                text = stringResource(R.string.ui_onboard_configure_complete),
                darkContainerColor = buttonDarkContainerColor,
                lightContainerColor = buttonLightContainerColor,
                darkContentColor = buttonDarkContentColor,
                lightContentColor = buttonLightContentColor,
                enabled = !uiState.isSaving,
                onClick = onComplete,
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
        is ConfigureInlineError.Validation -> stringResource(error.messageRes)
    }
}

private enum class ConfigureExpandableField {
    Endpoint,
    Model,
    ApiKey,
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
