package com.niki914.zafiro.app.ui.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.niki914.uikit.infra.ConfirmationLiquidDialog
import com.niki914.uikit.infra.ProvideLiquidScreenContentForPreview
import com.niki914.uikit.infra.component.SettingsGroupCard
import com.niki914.uikit.infra.component.SettingsItemDivider
import com.niki914.uikit.infra.nav.pageViewModel
import com.niki914.zafiro.app.R
import com.niki914.zafiro.app.ui.model.TextActionDeleteConfirmationState
import com.niki914.zafiro.app.ui.model.TextActionsEffect
import com.niki914.zafiro.app.ui.model.TextActionsInlineError
import com.niki914.zafiro.app.ui.model.TextActionsIntent
import com.niki914.zafiro.app.ui.model.TextActionsUiState
import com.niki914.zafiro.app.ui.model.TextActionsViewModel
import com.niki914.zafiro.app.ui.model.hasUnsavedChanges
import com.niki914.zafiro.app.ui.nav.TextActionDetailPage

@Composable
fun TextActionDetailContent(
    page: TextActionDetailPage,
    onBack: () -> Unit,
) {
    val viewModel = pageViewModel<TextActionsViewModel>()
    val uiState by viewModel.uiStateFlow.collectAsState()
    var requestedFocusField by rememberSaveable {
        mutableStateOf<TextActionEditableField?>(null)
    }

    EditableSettingsDetailChrome(
        isCreating = page.isCreating,
        hasUnsavedChanges = {
            uiState.formState.hasUnsavedChanges
        },
        onDiscardChanges = onBack,
        onDelete = {
            viewModel.sendIntent(TextActionsIntent.RequestDelete)
        },
        hasDeleteConfirmation = {
            uiState.deleteConfirmation != null
        },
        onDismissDeleteConfirmation = {
            viewModel.sendIntent(TextActionsIntent.DismissDeleteConfirmation)
        },
    ) {
        TextActionDetailContentBody(
            uiState = uiState,
            requestedFocusField = requestedFocusField,
            onRequestedFocusHandled = {
                requestedFocusField = null
            },
            onNameChange = { value ->
                viewModel.sendIntent(TextActionsIntent.NameChanged(value))
            },
            onPromptChange = { value ->
                viewModel.sendIntent(TextActionsIntent.PromptChanged(value))
            },
            onSave = {
                viewModel.sendIntent(TextActionsIntent.Save)
            },
        )

        TextActionDeleteConfirmationDialog(
            state = uiState.deleteConfirmation,
            onDismissRequest = {
                viewModel.sendIntent(TextActionsIntent.DismissDeleteConfirmation)
            },
            onConfirmClick = {
                viewModel.sendIntent(TextActionsIntent.ConfirmDelete)
            },
        )
    }

    LaunchedEffect(page.routeKey) {
        if (page.isCreating) {
            viewModel.sendIntent(TextActionsIntent.StartCreate)
        } else {
            viewModel.sendIntent(TextActionsIntent.Load)
        }
    }

    LaunchedEffect(page.routeKey, uiState.items.size, page.isCreating) {
        if (!page.isCreating && page.actionIndex in uiState.items.indices) {
            viewModel.sendIntent(TextActionsIntent.StartEdit(page.actionIndex))
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                TextActionsEffect.ExitDetail -> onBack()
                TextActionsEffect.FocusName -> {
                    requestedFocusField = TextActionEditableField.Name
                }

                TextActionsEffect.FocusPrompt -> {
                    requestedFocusField = TextActionEditableField.Prompt
                }

                TextActionsEffect.SilentNeedsCarrier -> Unit
            }
        }
    }
}

private enum class TextActionEditableField {
    Name,
    Prompt,
}

@Composable
private fun TextActionDetailContentBody(
    uiState: TextActionsUiState,
    requestedFocusField: TextActionEditableField?,
    onRequestedFocusHandled: () -> Unit,
    onNameChange: (String) -> Unit,
    onPromptChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    EditableSettingsDetailFormScaffold(
        actionText = stringResource(R.string.text_actions_save_action),
        requestedFocusField = requestedFocusField,
        onRequestedFocusHandled = onRequestedFocusHandled,
        onActionClick = onSave,
        description = stringResource(R.string.text_actions_editor_description),
        inlineErrorText = textActionsInlineErrorText(uiState.inlineError),
        actionEnabled = !uiState.isSaving,
    ) { fieldController ->
        SettingsGroupCard {
            SettingControlledExpandableTextItem(
                field = TextActionEditableField.Name,
                controller = fieldController,
                title = stringResource(R.string.text_actions_field_name),
                value = uiState.formState.name,
                onValueChange = onNameChange,
                placeholder = stringResource(R.string.text_actions_field_name_hint),
                description = textActionFieldErrorText(uiState.formState.nameErrorResId),
                enabled = !uiState.isSaving,
                minLines = 1,
                maxLines = 1,
            )
            SettingsItemDivider()
            SettingControlledExpandableTextItem(
                field = TextActionEditableField.Prompt,
                controller = fieldController,
                title = stringResource(R.string.text_actions_field_prompt),
                value = uiState.formState.promptInput,
                onValueChange = onPromptChange,
                placeholder = stringResource(R.string.text_actions_field_prompt_hint),
                description = textActionFieldErrorText(uiState.formState.promptErrorResId)
                    ?: stringResource(R.string.text_actions_field_prompt_description),
                enabled = !uiState.isSaving,
                minLines = 4,
                maxLines = 10,
            )
        }
    }
}

@Composable
private fun textActionsInlineErrorText(error: TextActionsInlineError?): String? {
    return when (error) {
        null -> null
        is TextActionsInlineError.LoadFailed -> error.message ?: stringResource(error.fallbackResId)
        is TextActionsInlineError.SaveFailed -> error.message ?: stringResource(error.fallbackResId)
        is TextActionsInlineError.DeleteFailed -> error.message ?: stringResource(error.fallbackResId)
    }
}

@Composable
private fun textActionFieldErrorText(errorResId: Int?): String? {
    return errorResId?.let { stringResource(it) }
}

@Composable
private fun TextActionDeleteConfirmationDialog(
    state: TextActionDeleteConfirmationState?,
    onDismissRequest: () -> Unit,
    onConfirmClick: () -> Unit,
) {
    ConfirmationLiquidDialog(
        visible = state != null,
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.text_actions_delete_dialog_title),
        text = stringResource(R.string.text_actions_delete_dialog_text, state?.value.orEmpty()),
        negativeButtonText = stringResource(R.string.delete_dialog_cancel),
        positiveButtonText = stringResource(R.string.delete_dialog_confirm),
        onNegativeClick = onDismissRequest,
        onPositiveClick = onConfirmClick,
    )
}

@Preview(showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun TextActionDetailContentPreview() {
    MaterialTheme {
        ProvideLiquidScreenContentForPreview(topPadding = 0.dp) {
            TextActionDetailContentBody(
                uiState = TextActionsUiState(),
                requestedFocusField = null,
                onRequestedFocusHandled = {},
                onNameChange = {},
                onPromptChange = {},
                onSave = {},
            )
        }
    }
}
