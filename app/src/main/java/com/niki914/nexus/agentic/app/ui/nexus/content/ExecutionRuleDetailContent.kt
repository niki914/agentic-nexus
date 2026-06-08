package com.niki914.nexus.agentic.app.ui.nexus.content

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.niki914.nexus.agentic.app.R
import com.niki914.nexus.agentic.app.ui.infra.ProvideLiquidScreenContentForPreview
import com.niki914.nexus.agentic.app.ui.infra.component.SettingExpandableTextItem
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsSegmentedSelector
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsDetailFormScaffold
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsGroupCard
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsItemDivider
import com.niki914.nexus.agentic.app.ui.infra.nav.pageViewModel
import com.niki914.nexus.agentic.app.ui.nexus.model.ExecutionRulesInlineError
import com.niki914.nexus.agentic.app.ui.nexus.model.ExecutionRulesSettingsEffect
import com.niki914.nexus.agentic.app.ui.nexus.model.ExecutionRulesSettingsIntent
import com.niki914.nexus.agentic.app.ui.nexus.model.ExecutionRulesSettingsUiState
import com.niki914.nexus.agentic.app.ui.nexus.model.ExecutionRulesSettingsViewModel
import com.niki914.nexus.agentic.app.ui.nexus.model.hasUnsavedChanges
import com.niki914.nexus.agentic.app.ui.nexus.nav.ExecutionRuleDetailPage
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeExecutionRuleEnabledMode

@Composable
fun ExecutionRuleDetailContent(
    page: ExecutionRuleDetailPage,
    onBack: () -> Unit,
) {
    val viewModel = pageViewModel<ExecutionRulesSettingsViewModel>()
    val uiState by viewModel.uiStateFlow.collectAsState()

    EditableSettingsDetailChrome(
        isCreating = page.isCreating,
        hasUnsavedChanges = {
            uiState.formState.hasUnsavedChanges
        },
        onDiscardChanges = onBack,
        onDelete = {
            viewModel.sendIntent(ExecutionRulesSettingsIntent.DeleteCurrent)
        },
    ) {
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
    }

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
    SettingsSegmentedSelector(
        title = stringResource(R.string.execution_rules_field_enabled_mode),
        options = RuntimeExecutionRuleEnabledMode.entries,
        selected = selectedMode,
        label = { option -> stringResource(option.labelRes()) },
        onSelected = onModeSelected,
    )
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
