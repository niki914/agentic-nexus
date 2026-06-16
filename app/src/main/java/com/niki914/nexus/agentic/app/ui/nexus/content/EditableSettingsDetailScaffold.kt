package com.niki914.nexus.agentic.app.ui.nexus.content

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsDetailFormScaffold
import com.niki914.nexus.agentic.app.ui.nexus.PageBackHandler
import com.niki914.nexus.agentic.app.ui.nexus.PageChromeContribution
import com.niki914.nexus.agentic.app.ui.nexus.RegisterPageChrome
import com.niki914.nexus.agentic.app.ui.nexus.nav.TopBarActionSpec

@Composable
fun EditableSettingsDetailChrome(
    isCreating: Boolean,
    hasUnsavedChanges: () -> Boolean,
    onDiscardChanges: () -> Unit,
    onDelete: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val latestHasUnsavedChanges by rememberUpdatedState(hasUnsavedChanges)
    val latestOnDelete by rememberUpdatedState(onDelete)
    val latestOnDiscardChanges by rememberUpdatedState(onDiscardChanges)
    var showUnsavedChangesDialog by rememberSaveable { mutableStateOf(false) }

    val pageChromeContribution = remember(isCreating) {
        PageChromeContribution(
            rightAction = if (isCreating || onDelete == null) {
                null
            } else {
                TopBarActionSpec(
                    icon = Icons.Default.Delete,
                    onClick = {
                        latestOnDelete?.invoke()
                    },
                )
            },
            backHandler = PageBackHandler(
                shouldConsumeBack = {
                    latestHasUnsavedChanges()
                },
                onConsumeBack = {
                    showUnsavedChangesDialog = true
                },
            ),
        )
    }
    RegisterPageChrome(pageChromeContribution)

    content()

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
            latestOnDiscardChanges()
        },
    )
}

@Composable
fun <Field : Enum<Field>> rememberEditableDetailFieldController(
    requestedFocusField: Field?,
    onRequestedFocusHandled: () -> Unit,
): EditableDetailFieldController<Field> {
    val focusManager = LocalFocusManager.current
    var expandedField by rememberSaveable { mutableStateOf<Field?>(null) }

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

    return EditableDetailFieldController(
        expandedField = expandedField,
        onExpandedFieldChange = { field ->
            expandedField = field
        },
        clearActiveField = ::clearActiveField,
    )
}

data class EditableDetailFieldController<Field>(
    val expandedField: Field?,
    val onExpandedFieldChange: (Field?) -> Unit,
    val clearActiveField: () -> Unit,
)

@Composable
fun <Field : Enum<Field>> EditableSettingsDetailFormScaffold(
    actionText: String,
    requestedFocusField: Field?,
    onRequestedFocusHandled: () -> Unit,
    onActionClick: () -> Unit,
    description: String? = null,
    inlineErrorText: String? = null,
    actionEnabled: Boolean = true,
    content: @Composable (EditableDetailFieldController<Field>) -> Unit,
) {
    val fieldController = rememberEditableDetailFieldController(
        requestedFocusField = requestedFocusField,
        onRequestedFocusHandled = onRequestedFocusHandled,
    )

    SettingsDetailFormScaffold(
        actionText = actionText,
        onActionClick = {
            fieldController.clearActiveField()
            onActionClick()
        },
        description = description,
        inlineErrorText = inlineErrorText,
        actionEnabled = actionEnabled,
        onBackgroundTap = fieldController.clearActiveField,
    ) {
        content(fieldController)
    }
}
