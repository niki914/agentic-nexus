package com.niki914.nexus.agentic.app.ui.nexus.content.mcp

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.niki914.nexus.agentic.app.R
import com.niki914.nexus.agentic.app.ui.infra.ConfirmationLiquidDialog
import com.niki914.nexus.agentic.app.ui.infra.ProvideLiquidScreenContentForPreview
import com.niki914.nexus.agentic.app.ui.infra.component.SettingExpandableTextItem
import com.niki914.nexus.agentic.app.ui.infra.component.SettingToggleItem
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsDetailFormScaffold
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsGroupCard
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsItemDivider
import com.niki914.nexus.agentic.app.ui.infra.nav.pageViewModel
import com.niki914.nexus.agentic.app.ui.nexus.PageBackHandler
import com.niki914.nexus.agentic.app.ui.nexus.PageChromeContribution
import com.niki914.nexus.agentic.app.ui.nexus.RegisterPageChrome
import com.niki914.nexus.agentic.app.ui.nexus.model.McpInlineError
import com.niki914.nexus.agentic.app.ui.nexus.model.McpSettingsEffect
import com.niki914.nexus.agentic.app.ui.nexus.model.McpSettingsIntent
import com.niki914.nexus.agentic.app.ui.nexus.model.McpSettingsUiState
import com.niki914.nexus.agentic.app.ui.nexus.model.McpSettingsViewModel
import com.niki914.nexus.agentic.app.ui.nexus.model.hasUnsavedChanges
import com.niki914.nexus.agentic.app.ui.nexus.nav.McpServerDetailPage
import com.niki914.nexus.agentic.app.ui.nexus.nav.TopBarActionSpec

@Composable
fun McpServerDetailContent(
    page: McpServerDetailPage,
    onBack: () -> Unit,
) {
    val viewModel = pageViewModel<McpSettingsViewModel>()
    val uiState by viewModel.uiStateFlow.collectAsState()
    val latestViewModel by rememberUpdatedState(viewModel)
    val latestUiState by rememberUpdatedState(uiState)
    val latestOnBack by rememberUpdatedState(onBack)
    var requestedFocusField by rememberSaveable { mutableStateOf<McpEditableField?>(null) }
    var showUnsavedChangesDialog by rememberSaveable { mutableStateOf(false) }

    val pageChromeContribution = remember(page.isCreating) {
        PageChromeContribution(
            rightAction = if (page.isCreating) {
                null
            } else {
                TopBarActionSpec(
                    icon = Icons.Default.Delete,
                    onClick = {
                        latestViewModel.sendIntent(McpSettingsIntent.DeleteCurrent)
                    },
                )
            },
            backHandler = PageBackHandler(
                shouldConsumeBack = {
                    latestUiState.formState.hasUnsavedChanges
                },
                onConsumeBack = {
                    showUnsavedChangesDialog = true
                },
            ),
        )
    }
    RegisterPageChrome(pageChromeContribution)

    LaunchedEffect(page.routeKey) {
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
                McpSettingsEffect.FocusName -> requestedFocusField = McpEditableField.Name
                McpSettingsEffect.FocusUrl -> requestedFocusField = McpEditableField.Url
                McpSettingsEffect.FocusHeaders -> requestedFocusField = McpEditableField.Headers
            }
        }
    }

    McpServerDetailContentBody(
        uiState = uiState,
        requestedFocusField = requestedFocusField,
        onRequestedFocusHandled = {
            requestedFocusField = null
        },
        onNameChange = { value ->
            viewModel.sendIntent(McpSettingsIntent.NameChanged(value))
        },
        onEnabledChange = { value ->
            viewModel.sendIntent(McpSettingsIntent.EnabledChanged(value))
        },
        onUrlChange = { value ->
            viewModel.sendIntent(McpSettingsIntent.UrlChanged(value))
        },
        onHeadersChange = { value ->
            viewModel.sendIntent(McpSettingsIntent.HeadersChanged(value))
        },
        onSave = {
            viewModel.sendIntent(McpSettingsIntent.Save)
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

@Composable
private fun McpServerDetailContentBody(
    uiState: McpSettingsUiState,
    requestedFocusField: McpEditableField?,
    onRequestedFocusHandled: () -> Unit,
    onNameChange: (String) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onUrlChange: (String) -> Unit,
    onHeadersChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var expandedField by rememberSaveable { mutableStateOf<McpEditableField?>(null) }

    fun clearActiveField() {
        expandedField = null
        focusManager.clearFocus()
    }

    LaunchedEffect(requestedFocusField) {
        if (requestedFocusField != null) {
            expandedField = requestedFocusField
            onRequestedFocusHandled()
        }
    }

    SettingsDetailFormScaffold(
        actionText = stringResource(R.string.mcp_save_action),
        onActionClick = {
            clearActiveField()
            onSave()
        },
        description = stringResource(R.string.mcp_page_description),
        inlineErrorText = mcpInlineErrorText(uiState.inlineError),
        actionEnabled = !uiState.isSaving,
        onBackgroundTap = ::clearActiveField,
    ) {
        McpIdentitySettingsBlock(
            uiState = uiState,
            expandedField = expandedField,
            onExpandedFieldChange = { field -> expandedField = field },
            onNameChange = onNameChange,
            onEnabledChange = {
                clearActiveField()
                onEnabledChange(it)
            },
        )

        McpConnectionSettingsBlock(
            uiState = uiState,
            expandedField = expandedField,
            onExpandedFieldChange = { field -> expandedField = field },
            onUrlChange = onUrlChange,
            onHeadersChange = onHeadersChange,
        )
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
            placeholder = stringResource(R.string.mcp_field_name_hint),
            description = mcpFieldErrorText(uiState.formState.nameErrorResId),
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
        SettingsItemDivider()
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
            placeholder = stringResource(R.string.mcp_field_url_hint),
            description = mcpFieldErrorText(uiState.formState.urlErrorResId),
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
        SettingsItemDivider()
        SettingExpandableTextItem(
            title = stringResource(R.string.mcp_field_headers),
            value = uiState.formState.headersInput,
            onValueChange = onHeadersChange,
            placeholder = stringResource(R.string.mcp_field_headers_hint),
            description = mcpFieldErrorText(uiState.formState.headersErrorResId),
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
    }
}

@Composable
private fun mcpFieldErrorText(errorResId: Int?): String? {
    return errorResId?.let { stringResource(id = it) }
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

@Preview(showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun McpServerDetailContentPreview() {
    MaterialTheme {
        ProvideLiquidScreenContentForPreview(topPadding = 0.dp) {
            McpServerDetailContentBody(
                uiState = McpSettingsUiState(
                    items = emptyList(),
                    formState = com.niki914.nexus.agentic.app.ui.nexus.model.McpServerFormState(
                        name = "demo-mcp",
                        url = "https://a.b.c/mcp",
                        enabled = true,
                        headersInput = "{\n  \"Authorization\": \"Bearer xxx\"\n}",
                        headersErrorResId = R.string.mcp_error_headers_not_object,
                    ),
                    isLoading = false,
                    isSaving = false,
                ),
                requestedFocusField = McpEditableField.Headers,
                onRequestedFocusHandled = {},
                onNameChange = {},
                onEnabledChange = {},
                onUrlChange = {},
                onHeadersChange = {},
                onSave = {},
            )
        }
    }
}
