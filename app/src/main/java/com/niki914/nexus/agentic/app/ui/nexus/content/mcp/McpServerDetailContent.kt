package com.niki914.nexus.agentic.app.ui.nexus.content.mcp

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.niki914.nexus.agentic.app.R
import com.niki914.nexus.agentic.app.ui.infra.component.SettingExpandableTextItem
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsGroupCard
import com.niki914.nexus.agentic.app.ui.infra.component.SettingToggleItem
import com.niki914.nexus.agentic.app.ui.infra.component.TintLiquidButton
import com.niki914.nexus.agentic.app.ui.infra.nav.pageViewModel
import com.niki914.nexus.agentic.app.ui.nexus.model.McpInlineError
import com.niki914.nexus.agentic.app.ui.nexus.model.McpSettingsEffect
import com.niki914.nexus.agentic.app.ui.nexus.model.McpSettingsIntent
import com.niki914.nexus.agentic.app.ui.nexus.model.McpSettingsUiState
import com.niki914.nexus.agentic.app.ui.nexus.model.McpSettingsViewModel
import com.niki914.nexus.agentic.app.ui.nexus.nav.McpServerDetailPage
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@Composable
fun McpServerDetailContent(
    page: McpServerDetailPage,
    topPadding: Dp,
    hazeState: HazeState,
    onBack: () -> Unit,
) {
    val viewModel = pageViewModel<McpSettingsViewModel>(factory = McpSettingsViewModelFactory)
    val uiState by viewModel.uiStateFlow.collectAsState()
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    var expandedField by rememberSaveable { mutableStateOf<McpEditableField?>(null) }

    fun clearActiveField() {
        expandedField = null
        focusManager.clearFocus()
    }

    LaunchedEffect(page.routeKey) {
        clearActiveField()
        if (page.isCreating) {
            viewModel.sendIntent(McpSettingsIntent.StartCreate)
        } else {
            viewModel.sendIntent(McpSettingsIntent.Load)
        }
    }

    LaunchedEffect(page.routeKey, uiState.items.size, page.isCreating) {
        if (!page.isCreating && page.serverIndex in uiState.items.indices) {
            viewModel.sendIntent(McpSettingsIntent.StartEdit(page.serverIndex))
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                McpSettingsEffect.ExitDetail -> onBack()
            }
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
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { clearActiveField() })
                },
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text(
                    text = stringResource(R.string.mcp_page_description),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                McpIdentitySettingsBlock(
                    uiState = uiState,
                    expandedField = expandedField,
                    onExpandedFieldChange = { field -> expandedField = field },
                    onNameChange = { value ->
                        viewModel.sendIntent(McpSettingsIntent.NameChanged(value))
                    },
                    onEnabledChange = { value ->
                        viewModel.sendIntent(McpSettingsIntent.EnabledChanged(value))
                    },
                )

                McpConnectionSettingsBlock(
                    uiState = uiState,
                    expandedField = expandedField,
                    onExpandedFieldChange = { field -> expandedField = field },
                    onUrlChange = { value ->
                        viewModel.sendIntent(McpSettingsIntent.UrlChanged(value))
                    },
                    onHeadersChange = { value ->
                        viewModel.sendIntent(McpSettingsIntent.HeadersChanged(value))
                    },
                )

                mcpInlineErrorText(uiState.inlineError)?.let { errorText ->
                    Text(
                        text = errorText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            TintLiquidButton(
                text = stringResource(R.string.mcp_save_action),
                enabled = !uiState.isSaving,
                onClick = {
                    clearActiveField()
                    viewModel.sendIntent(McpSettingsIntent.Save)
                },
            )
        }
    }
}

private enum class McpEditableField {
    Name,
    Url,
    Headers,
}

@Composable
private fun McpIdentitySettingsBlock(
    uiState: McpSettingsUiState,
    expandedField: McpEditableField?,
    onExpandedFieldChange: (McpEditableField?) -> Unit,
    onNameChange: (String) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
) {
    SettingsGroupCard {
        SettingExpandableTextItem(
            title = stringResource(R.string.mcp_field_name),
            value = uiState.formState.name,
            onValueChange = onNameChange,
            placeholder = stringResource(R.string.mcp_field_name_placeholder),
            description = null,
            enabled = !uiState.isSaving,
            minLines = 1,
            maxLines = 1,
            expanded = expandedField == McpEditableField.Name,
            onExpandedChange = { isExpanded ->
                onExpandedFieldChange(
                    if (isExpanded) McpEditableField.Name else null,
                )
            },
        )
        McpSupportingText(
            errorText = uiState.formState.nameError,
            hintText = stringResource(R.string.mcp_field_name_placeholder),
        )
        McpItemDivider()
        SettingToggleItem(
            title = stringResource(R.string.mcp_field_enabled),
            checked = uiState.formState.enabled,
            enabled = !uiState.isSaving,
            onCheckedChange = onEnabledChange,
        )
    }
}

@Composable
private fun McpConnectionSettingsBlock(
    uiState: McpSettingsUiState,
    expandedField: McpEditableField?,
    onExpandedFieldChange: (McpEditableField?) -> Unit,
    onUrlChange: (String) -> Unit,
    onHeadersChange: (String) -> Unit,
) {
    SettingsGroupCard {
        SettingExpandableTextItem(
            title = stringResource(R.string.mcp_field_url),
            value = uiState.formState.url,
            onValueChange = onUrlChange,
            placeholder = stringResource(R.string.mcp_field_url_placeholder),
            description = null,
            enabled = !uiState.isSaving,
            minLines = 1,
            maxLines = 1,
            expanded = expandedField == McpEditableField.Url,
            onExpandedChange = { isExpanded ->
                onExpandedFieldChange(
                    if (isExpanded) McpEditableField.Url else null,
                )
            },
        )
        McpSupportingText(
            errorText = uiState.formState.urlError,
            hintText = stringResource(R.string.mcp_field_url_placeholder),
        )
        McpItemDivider()
        SettingExpandableTextItem(
            title = stringResource(R.string.mcp_field_headers),
            value = uiState.formState.headersInput,
            onValueChange = onHeadersChange,
            placeholder = stringResource(R.string.mcp_field_headers_placeholder),
            description = null,
            enabled = !uiState.isSaving,
            minLines = 4,
            maxLines = 10,
            expanded = expandedField == McpEditableField.Headers,
            onExpandedChange = { isExpanded ->
                onExpandedFieldChange(
                    if (isExpanded) McpEditableField.Headers else null,
                )
            },
        )
        McpSupportingText(
            errorText = uiState.formState.headersError,
            hintText = stringResource(R.string.mcp_field_headers_placeholder),
        )
    }
}

@Composable
private fun McpSupportingText(
    errorText: String?,
    hintText: String,
) {
    Text(
        text = errorText ?: hintText,
        style = MaterialTheme.typography.bodySmall,
        color = if (errorText == null) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.error
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
    )
}

@Composable
private fun McpItemDivider() {
    HorizontalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        modifier = Modifier.padding(horizontal = 12.dp),
    )
}

@Composable
private fun mcpInlineErrorText(error: McpInlineError?): String? {
    return when (error) {
        null -> null
        is McpInlineError.LoadFailed -> error.message
        is McpInlineError.SaveFailed -> error.message
        is McpInlineError.DeleteFailed -> error.message
    }
}

internal object McpSettingsViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == McpSettingsViewModel::class.java)
        return McpSettingsViewModel() as T
    }
}
