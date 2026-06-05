package com.niki914.nexus.agentic.app.ui.nexus.content

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import com.niki914.nexus.agentic.app.R
import com.niki914.nexus.agentic.app.ui.infra.ConfirmationLiquidDialog
import com.niki914.nexus.agentic.app.ui.infra.component.SettingExpandableTextItem
import com.niki914.nexus.agentic.app.ui.infra.component.SettingToggleItem
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsDetailFormScaffold
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsGroupCard
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsItemDivider
import com.niki914.nexus.agentic.app.ui.infra.nav.pageViewModel
import com.niki914.nexus.agentic.app.ui.nexus.PageBackHandler
import com.niki914.nexus.agentic.app.ui.nexus.PageChromeContribution
import com.niki914.nexus.agentic.app.ui.nexus.RegisterPageChrome
import com.niki914.nexus.agentic.app.ui.nexus.model.CustomToolInlineError
import com.niki914.nexus.agentic.app.ui.nexus.model.CustomToolSettingsEffect
import com.niki914.nexus.agentic.app.ui.nexus.model.CustomToolSettingsIntent
import com.niki914.nexus.agentic.app.ui.nexus.model.CustomToolSettingsUiState
import com.niki914.nexus.agentic.app.ui.nexus.model.CustomToolSettingsViewModel
import com.niki914.nexus.agentic.app.ui.nexus.model.hasUnsavedChanges
import com.niki914.nexus.agentic.app.ui.nexus.nav.CustomToolDetailPage
import com.niki914.nexus.agentic.app.ui.nexus.nav.TopBarActionSpec

@Composable
fun CustomToolDetailContent(
    page: CustomToolDetailPage,
    onBack: () -> Unit,
) {
    val viewModel = pageViewModel<CustomToolSettingsViewModel>()
    val uiState by viewModel.uiStateFlow.collectAsState()
    val latestViewModel by rememberUpdatedState(viewModel)
    val latestUiState by rememberUpdatedState(uiState)
    val latestOnBack by rememberUpdatedState(onBack)
    var requestedFocusField by rememberSaveable {
        mutableStateOf<CustomToolEditableField?>(null)
    }
    var showUnsavedChangesDialog by rememberSaveable { mutableStateOf(false) }

    val pageChromeContribution = remember(page.isCreating) {
        PageChromeContribution(
            rightAction = if (page.isCreating) {
                null
            } else {
                TopBarActionSpec(
                    icon = Icons.Default.Delete,
                    onClick = {
                        latestViewModel.sendIntent(CustomToolSettingsIntent.DeleteCurrent)
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
            viewModel.sendIntent(CustomToolSettingsIntent.StartCreate)
        } else {
            viewModel.sendIntent(CustomToolSettingsIntent.Load)
        }
    }

    LaunchedEffect(page.routeKey, uiState.items.size, page.isCreating) {
        if (!page.isCreating && page.toolIndex in uiState.items.indices) {
            viewModel.sendIntent(CustomToolSettingsIntent.StartEdit(page.toolIndex))
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                CustomToolSettingsEffect.ExitDetail -> onBack()
                CustomToolSettingsEffect.FocusName -> {
                    requestedFocusField = CustomToolEditableField.Name
                }

                CustomToolSettingsEffect.FocusDescription -> {
                    requestedFocusField = CustomToolEditableField.Description
                }

                CustomToolSettingsEffect.FocusCommand -> {
                    requestedFocusField = CustomToolEditableField.Command
                }
            }
        }
    }

    CustomToolDetailContentBody(
        uiState = uiState,
        requestedFocusField = requestedFocusField,
        onRequestedFocusHandled = {
            requestedFocusField = null
        },
        onNameChange = { value ->
            viewModel.sendIntent(CustomToolSettingsIntent.NameChanged(value))
        },
        onDescriptionChange = { value ->
            viewModel.sendIntent(CustomToolSettingsIntent.DescriptionChanged(value))
        },
        onCommandChange = { value ->
            viewModel.sendIntent(CustomToolSettingsIntent.CommandChanged(value))
        },
        onEnabledChange = { value ->
            viewModel.sendIntent(CustomToolSettingsIntent.EnabledChanged(value))
        },
        onSave = {
            viewModel.sendIntent(CustomToolSettingsIntent.Save)
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
private fun CustomToolDetailContentBody(
    uiState: CustomToolSettingsUiState,
    requestedFocusField: CustomToolEditableField?,
    onRequestedFocusHandled: () -> Unit,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCommandChange: (String) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onSave: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var expandedField by rememberSaveable {
        mutableStateOf<CustomToolEditableField?>(null)
    }

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
        actionText = stringResource(R.string.custom_tool_save_action),
        onActionClick = {
            clearActiveField()
            onSave()
        },
        description = stringResource(R.string.custom_tool_editor_description),
        inlineErrorText = customToolInlineErrorText(uiState.inlineError),
        actionEnabled = !uiState.isSaving,
        onBackgroundTap = ::clearActiveField,
    ) {
        CustomToolIdentitySettingsBlock(
            uiState = uiState,
            expandedField = expandedField,
            onExpandedFieldChange = { field -> expandedField = field },
            onNameChange = onNameChange,
            onDescriptionChange = onDescriptionChange,
            onEnabledChange = {
                clearActiveField()
                onEnabledChange(it)
            },
        )

        CustomToolCommandSettingsBlock(
            uiState = uiState,
            expandedField = expandedField,
            onExpandedFieldChange = { field -> expandedField = field },
            onCommandChange = onCommandChange,
        )
    }
}

private enum class CustomToolEditableField {
    Name,
    Description,
    Command,
}

@Composable
private fun CustomToolIdentitySettingsBlock(
    uiState: CustomToolSettingsUiState,
    expandedField: CustomToolEditableField?,
    onExpandedFieldChange: (CustomToolEditableField?) -> Unit,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
) {
    SettingsGroupCard {
        SettingExpandableTextItem(
            title = stringResource(R.string.custom_tool_field_name),
            value = uiState.formState.name,
            onValueChange = onNameChange,
            placeholder = stringResource(R.string.custom_tool_field_name_hint),
            description = customToolFieldErrorText(uiState.formState.nameErrorResId),
            enabled = !uiState.isSaving,
            minLines = 1,
            maxLines = 1,
            expanded = expandedField == CustomToolEditableField.Name,
            onExpandedChange = { isExpanded ->
                onExpandedFieldChange(
                    if (isExpanded) CustomToolEditableField.Name else null,
                )
            },
        )
        SettingsItemDivider()
        SettingExpandableTextItem(
            title = stringResource(R.string.custom_tool_field_description),
            value = uiState.formState.description,
            onValueChange = onDescriptionChange,
            placeholder = stringResource(R.string.custom_tool_field_description_hint),
            description = customToolFieldErrorText(uiState.formState.descriptionErrorResId),
            enabled = !uiState.isSaving,
            minLines = 3,
            maxLines = 8,
            expanded = expandedField == CustomToolEditableField.Description,
            onExpandedChange = { isExpanded ->
                onExpandedFieldChange(
                    if (isExpanded) CustomToolEditableField.Description else null,
                )
            },
        )
        SettingsItemDivider()
        SettingToggleItem(
            title = stringResource(R.string.custom_tool_field_enabled),
            checked = uiState.formState.enabled,
            enabled = !uiState.isSaving,
            onCheckedChange = onEnabledChange,
        )
    }
}

@Composable
private fun CustomToolCommandSettingsBlock(
    uiState: CustomToolSettingsUiState,
    expandedField: CustomToolEditableField?,
    onExpandedFieldChange: (CustomToolEditableField?) -> Unit,
    onCommandChange: (String) -> Unit,
) {
    SettingsGroupCard {
        SettingExpandableTextItem(
            title = stringResource(R.string.custom_tool_field_command),
            value = uiState.formState.command,
            onValueChange = onCommandChange,
            placeholder = stringResource(R.string.custom_tool_field_command_hint),
            description = uiState.formState.commandErrorMessage
                ?: customToolFieldErrorText(uiState.formState.commandErrorResId),
            enabled = !uiState.isSaving,
            minLines = 3,
            maxLines = 10,
            expanded = expandedField == CustomToolEditableField.Command,
            onExpandedChange = { isExpanded ->
                onExpandedFieldChange(
                    if (isExpanded) CustomToolEditableField.Command else null,
                )
            },
        )
    }
}

@Composable
private fun customToolFieldErrorText(errorResId: Int?): String? {
    return errorResId?.let { stringResource(id = it) }
}

@Composable
private fun customToolInlineErrorText(error: CustomToolInlineError?): String? {
    return when (error) {
        null -> null
        is CustomToolInlineError.LoadFailed -> stringResource(
            R.string.custom_tool_error_load_failed,
            error.message,
        )

        is CustomToolInlineError.SaveFailed -> stringResource(
            R.string.custom_tool_error_save_failed,
            error.message,
        )

        is CustomToolInlineError.DeleteFailed -> stringResource(
            R.string.custom_tool_error_delete_failed,
            error.message,
        )
    }
}
