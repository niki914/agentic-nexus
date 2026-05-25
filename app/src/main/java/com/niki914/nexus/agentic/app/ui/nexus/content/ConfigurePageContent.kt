package com.niki914.nexus.agentic.app.ui.nexus.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.niki914.nexus.agentic.app.R
import com.niki914.nexus.agentic.app.liquid_example.components.LiquidToggle
import com.niki914.nexus.agentic.app.ui.infra.component.LiquidSecretTextField
import com.niki914.nexus.agentic.app.ui.infra.component.LiquidTextField
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsGroupCard
import com.niki914.nexus.agentic.app.ui.infra.component.TintLiquidButton
import com.niki914.nexus.agentic.app.ui.nexus.model.ConfigureInlineError
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
    onToggleApiKeyVisibility: () -> Unit,
    onComplete: () -> Unit,
) {
    val scrollState = rememberScrollState()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .hazeSource(hazeState)
            .padding(top = topPadding)
            .padding(horizontal = 16.dp, vertical = 24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = stringResource(R.string.ui_onboard_configure_headline),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(
                            R.string.ui_onboard_configure_description,
                            uiState.providerSpec.brandName,
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                SettingsGroupCard(title = uiState.providerSpec.brandName) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            text = uiState.providerSpec.officialEndpoint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        EndpointOverrideRow(
                            label = stringResource(R.string.ui_onboard_configure_endpoint_override_title),
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
                        )
                        ConfigureField(
                            label = stringResource(R.string.ui_onboard_configure_endpoint_label),
                        ) {
                            LiquidTextField(
                                value = uiState.endpointInput,
                                onValueChange = onEndpointChange,
                                placeholder = stringResource(R.string.ui_onboard_configure_endpoint_placeholder),
                                enabled = uiState.endpointOverrideEnabled && !uiState.isSaving,
                                singleLine = true,
                            )
                        }
                        ConfigureField(
                            label = stringResource(R.string.ui_onboard_configure_model_label),
                        ) {
                            LiquidTextField(
                                value = uiState.modelInput,
                                onValueChange = onModelChange,
                                placeholder = stringResource(R.string.ui_onboard_configure_model_placeholder),
                                enabled = !uiState.isSaving,
                                singleLine = true,
                            )
                        }
                        ConfigureField(
                            label = stringResource(R.string.ui_onboard_configure_api_key_label),
                        ) {
                            LiquidSecretTextField(
                                value = uiState.apiKeyInput,
                                onValueChange = onApiKeyChange,
                                placeholder = stringResource(R.string.ui_onboard_configure_api_key_placeholder),
                                visible = uiState.apiKeyVisible,
                                onToggleVisibility = onToggleApiKeyVisibility,
                                toggleVisibleContentDescription = stringResource(
                                    R.string.ui_onboard_configure_api_key_show,
                                ),
                                toggleHiddenContentDescription = stringResource(
                                    R.string.ui_onboard_configure_api_key_hide,
                                ),
                                enabled = !uiState.isSaving,
                            )
                        }
                        configureInlineErrorText(uiState.inlineError)?.let { errorText ->
                            Text(
                                text = errorText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
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
private fun ConfigureField(
    label: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

@Composable
private fun EndpointOverrideRow(
    label: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    val backdrop = rememberCanvasBackdrop {
        drawRect(color)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LiquidToggle(
            selected = { checked },
            onSelect = onCheckedChange,
            backdrop = backdrop,
            modifier = Modifier.alpha(if (enabled) 1f else 0.5f),
        )
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
