package com.niki914.nexus.agentic.app.ui.nexus.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.niki914.nexus.agentic.app.R
import com.niki914.nexus.agentic.app.ui.infra.ConfirmationLiquidDialog
import com.niki914.nexus.agentic.app.ui.infra.ProvideLiquidScreenContentForPreview
import com.niki914.nexus.agentic.app.ui.infra.component.SettingExpandableTextItem
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsDetailFormScaffold
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsGroupCard
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsItemDivider
import com.niki914.nexus.agentic.app.ui.infra.nav.pageViewModel
import com.niki914.nexus.agentic.app.ui.nexus.PageBackHandler
import com.niki914.nexus.agentic.app.ui.nexus.PageChromeContribution
import com.niki914.nexus.agentic.app.ui.nexus.RegisterPageChrome
import com.niki914.nexus.agentic.app.ui.nexus.model.ExecutionRulesInlineError
import com.niki914.nexus.agentic.app.ui.nexus.model.ExecutionRulesSettingsEffect
import com.niki914.nexus.agentic.app.ui.nexus.model.ExecutionRulesSettingsIntent
import com.niki914.nexus.agentic.app.ui.nexus.model.ExecutionRulesSettingsUiState
import com.niki914.nexus.agentic.app.ui.nexus.model.ExecutionRulesSettingsViewModel
import com.niki914.nexus.agentic.app.ui.nexus.model.hasUnsavedChanges
import com.niki914.nexus.agentic.app.ui.nexus.nav.ExecutionRuleDetailPage
import com.niki914.nexus.agentic.app.ui.nexus.nav.TopBarActionSpec
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeExecutionRuleEnabledMode

@Composable
fun ExecutionRuleDetailContent(
    page: ExecutionRuleDetailPage,
    onBack: () -> Unit,
) {
    val viewModel = pageViewModel<ExecutionRulesSettingsViewModel>()
    val uiState by viewModel.uiStateFlow.collectAsState()
    val latestViewModel by rememberUpdatedState(viewModel)
    val latestUiState by rememberUpdatedState(uiState)
    val latestOnBack by rememberUpdatedState(onBack)
    var showUnsavedChangesDialog by rememberSaveable { mutableStateOf(false) }

    val pageChromeContribution = remember(page.isCreating) {
        PageChromeContribution(
            rightAction = if (page.isCreating) {
                null
            } else {
                TopBarActionSpec(
                    icon = Icons.Default.Delete,
                    onClick = {
                        latestViewModel.sendIntent(ExecutionRulesSettingsIntent.DeleteCurrent)
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
            viewModel.sendIntent(ExecutionRulesSettingsIntent.StartCreate)
        } else {
            viewModel.sendIntent(ExecutionRulesSettingsIntent.Load)
        }
    }

    LaunchedEffect(page.routeKey, uiState.items.size, page.isCreating) {
        if (!page.isCreating && page.ruleIndex in uiState.items.indices) {
            viewModel.sendIntent(ExecutionRulesSettingsIntent.StartEdit(page.ruleIndex))
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                ExecutionRulesSettingsEffect.ExitDetail -> onBack()
            }
        }
    }

    ExecutionRuleDetailContentBody(
        uiState = uiState,
        onNameChange = { value ->
            viewModel.sendIntent(ExecutionRulesSettingsIntent.NameChanged(value))
        },
        onEnabledModeChange = { value ->
            viewModel.sendIntent(ExecutionRulesSettingsIntent.EnabledModeChanged(value))
        },
        onPatternsInputChange = { value ->
            viewModel.sendIntent(ExecutionRulesSettingsIntent.PatternsChanged(value))
        },
        onSave = {
            viewModel.sendIntent(ExecutionRulesSettingsIntent.Save)
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
private fun ExecutionRuleDetailContentBody(
    uiState: ExecutionRulesSettingsUiState,
    onNameChange: (String) -> Unit,
    onEnabledModeChange: (RuntimeExecutionRuleEnabledMode) -> Unit,
    onPatternsInputChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    fun clearFocus() {
        focusManager.clearFocus()
    }

    SettingsDetailFormScaffold(
        actionText = stringResource(R.string.execution_rules_save_action),
        onActionClick = {
            clearFocus()
            onSave()
        },
        description = stringResource(R.string.execution_rules_editor_description),
        inlineErrorText = executionRulesInlineErrorText(uiState.inlineError),
        actionEnabled = !uiState.isSaving,
        onBackgroundTap = ::clearFocus,
    ) {
        SettingsGroupCard {
            SettingExpandableTextItem(
                title = stringResource(R.string.execution_rules_field_name),
                value = uiState.formState.name,
                onValueChange = onNameChange,
                placeholder = stringResource(R.string.execution_rules_field_name_hint),
                enabled = !uiState.isSaving,
                minLines = 1,
                maxLines = 1,
            )
            SettingsItemDivider()
            ExecutionRuleEnabledModeSection(
                selectedMode = uiState.formState.enabledMode,
                onModeSelected = onEnabledModeChange,
            )
        }

        SettingsGroupCard {
            SettingExpandableTextItem(
                title = stringResource(R.string.execution_rules_field_patterns),
                value = uiState.formState.patternsInput,
                onValueChange = onPatternsInputChange,
                placeholder = stringResource(R.string.execution_rules_field_patterns_hint),
                description = stringResource(R.string.execution_rules_field_patterns_description),
                enabled = !uiState.isSaving,
                minLines = 4,
                maxLines = 6,
            )
        }
    }
}

@Composable
private fun ExecutionRuleEnabledModeSection(
    selectedMode: RuntimeExecutionRuleEnabledMode,
    onModeSelected: (RuntimeExecutionRuleEnabledMode) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.execution_rules_field_enabled_mode),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth(),
        ) {
            val options = RuntimeExecutionRuleEnabledMode.entries
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == selectedMode,
                    onClick = { onModeSelected(option) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = options.size,
                    ),
                    label = {
                        Text(
                            text = stringResource(option.labelRes()),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun executionRulesInlineErrorText(error: ExecutionRulesInlineError?): String? {
    return when (error) {
        null -> null
        is ExecutionRulesInlineError.LoadFailed -> error.message
        is ExecutionRulesInlineError.SaveFailed -> error.message
        is ExecutionRulesInlineError.DeleteFailed -> error.message
    }
}

private fun RuntimeExecutionRuleEnabledMode.labelRes(): Int {
    return when (this) {
        RuntimeExecutionRuleEnabledMode.ALWAYS -> R.string.execution_rules_enabled_mode_enabled
        RuntimeExecutionRuleEnabledMode.LOCKED_ONLY -> R.string.execution_rules_enabled_mode_locked_only
        RuntimeExecutionRuleEnabledMode.DISABLED -> R.string.execution_rules_enabled_mode_disabled
    }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun ExecutionRuleDetailContentPreview() {
    MaterialTheme {
        ProvideLiquidScreenContentForPreview(topPadding = 0.dp) {
            ExecutionRuleDetailContentBody(
                uiState = ExecutionRulesSettingsUiState(
                    formState = com.niki914.nexus.agentic.app.ui.nexus.model.ExecutionRuleFormState(
                        name = "危险删改",
                        enabledMode = RuntimeExecutionRuleEnabledMode.LOCKED_ONLY,
                        patternsInput = """
                            \brm\s+-rf\b
                            \bmkfs\b
                            pm uninstall
                        """.trimIndent(),
                    ),
                    isLoading = false,
                    isSaving = false,
                ),
                onNameChange = {},
                onEnabledModeChange = {},
                onPatternsInputChange = {},
                onSave = {},
            )
        }
    }
}
