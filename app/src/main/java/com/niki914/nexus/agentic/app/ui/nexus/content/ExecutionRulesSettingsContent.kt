package com.niki914.nexus.agentic.app.ui.nexus.content

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.niki914.nexus.agentic.app.R
import com.niki914.nexus.agentic.app.ui.infra.LiquidDialog
import com.niki914.nexus.agentic.app.ui.infra.component.LiquidTextField
import com.niki914.nexus.agentic.app.ui.infra.component.MaterialTintLiquidButton
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsGroupCard
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsListItem
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsListPageContent
import com.niki914.nexus.agentic.app.ui.infra.nav.pageViewModel
import com.niki914.nexus.agentic.app.ui.nexus.PageBackHandler
import com.niki914.nexus.agentic.app.ui.nexus.PageChromeContribution
import com.niki914.nexus.agentic.app.ui.nexus.RegisterPageChrome
import com.niki914.nexus.agentic.app.ui.nexus.model.ExecutionRuleDraft
import com.niki914.nexus.agentic.app.ui.nexus.model.ExecutionRuleMatcherType
import com.niki914.nexus.agentic.app.ui.nexus.model.ExecutionRuleMode
import com.niki914.nexus.agentic.app.ui.nexus.model.ExecutionRulesSettingsIntent
import com.niki914.nexus.agentic.app.ui.nexus.model.ExecutionRulesSettingsUiState
import com.niki914.nexus.agentic.app.ui.nexus.model.ExecutionRulesSettingsViewModel
import com.niki914.nexus.agentic.app.ui.nexus.nav.TopBarActionSpec

@Composable
fun ExecutionRulesSettingsContent() {
    val viewModel = pageViewModel<ExecutionRulesSettingsViewModel>()
    val uiState by viewModel.uiStateFlow.collectAsState()
    val latestUiState by rememberUpdatedState(uiState)
    val latestViewModel by rememberUpdatedState(viewModel)
    val pageChromeContribution = remember(viewModel) {
        PageChromeContribution(
            rightAction = TopBarActionSpec(
                icon = Icons.Default.Add,
                onClick = {
                    viewModel.sendIntent(ExecutionRulesSettingsIntent.StartCreate)
                },
            ),
            backHandler = PageBackHandler(
                shouldConsumeBack = {
                    latestUiState.editingRule != null
                },
                onConsumeBack = {
                    latestViewModel.sendIntent(ExecutionRulesSettingsIntent.DismissEditingRule)
                },
            ),
        )
    }
    RegisterPageChrome(pageChromeContribution)

    LaunchedEffect(Unit) {
        viewModel.sendIntent(ExecutionRulesSettingsIntent.Load)
    }

    ExecutionRulesSettingsContentBody(
        uiState = uiState,
        onStartEdit = { id ->
            viewModel.sendIntent(ExecutionRulesSettingsIntent.StartEdit(id))
        },
        onIntent = viewModel::sendIntent,
    )
}

@Composable
private fun ExecutionRulesSettingsContentBody(
    uiState: ExecutionRulesSettingsUiState,
    onStartEdit: (String) -> Unit,
    onIntent: (ExecutionRulesSettingsIntent) -> Unit,
) {
    SettingsListPageContent(
        description = stringResource(R.string.execution_rules_page_description),
    ) {
        if (uiState.rules.isEmpty()) {
            SettingsGroupCard {
                ExecutionRulesListMessage(text = stringResource(R.string.execution_rules_empty))
                ExecutionRulesListMessage(
                    text = stringResource(R.string.execution_rules_empty_action_hint)
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                uiState.rules.forEach { rule ->
                    ExecutionRuleCard(
                        rule = rule,
                        onClick = {
                            onStartEdit(rule.id)
                        },
                    )
                }
            }
        }
    }

    ExecutionRuleDetailSheet(
        rule = uiState.editingRule,
        onIntent = onIntent,
    )
}

@Composable
private fun ExecutionRuleCard(
    rule: ExecutionRuleDraft,
    onClick: () -> Unit,
) {
    SettingsGroupCard {
        SettingsListItem(
            title = rule.note,
            showChevron = true,
            leadingContent = {
                ExecutionRuleModeBadge(mode = rule.mode)
            },
            onClick = onClick,
        )
    }
}

@Composable
private fun ExecutionRuleDetailSheet(
    rule: ExecutionRuleDraft?,
    onIntent: (ExecutionRulesSettingsIntent) -> Unit,
) {
    LiquidDialog(
        visible = rule != null,
        onDismissRequest = {
            onIntent(ExecutionRulesSettingsIntent.DismissEditingRule)
        },
        title = {
            Text(
                text = stringResource(
                    if (rule?.note.isNullOrBlank()) {
                        R.string.execution_rules_editor_title_create
                    } else {
                        R.string.execution_rules_editor_title_edit
                    }
                ),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        content = {
            if (rule != null) {
                ExecutionRuleModeSegmentedControl(
                    selected = rule.mode,
                    onSelected = { mode ->
                        onIntent(ExecutionRulesSettingsIntent.ModeChanged(mode))
                    },
                )
                ExecutionRulesFieldLabel(text = stringResource(R.string.execution_rules_field_note))
                LiquidTextField(
                    value = rule.note,
                    onValueChange = { value ->
                        onIntent(ExecutionRulesSettingsIntent.NoteChanged(value))
                    },
                    placeholder = stringResource(R.string.execution_rules_field_note_hint),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                ExecutionRuleMatcherSegmentedControl(
                    selected = rule.matcherType,
                    onSelected = { matcherType ->
                        onIntent(ExecutionRulesSettingsIntent.MatcherTypeChanged(matcherType))
                    },
                )
                ExecutionRulesFieldLabel(
                    text = stringResource(R.string.execution_rules_field_keywords)
                )
                LiquidTextField(
                    value = rule.keywords.joinToString(separator = "\n"),
                    onValueChange = { value ->
                        onIntent(
                            ExecutionRulesSettingsIntent.KeywordsChanged(
                                value.lines(),
                            )
                        )
                    },
                    placeholder = stringResource(R.string.execution_rules_field_keywords_hint),
                    enabled = rule.matcherType == ExecutionRuleMatcherType.Keywords,
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                ExecutionRulesFieldLabel(text = stringResource(R.string.execution_rules_field_regex))
                LiquidTextField(
                    value = rule.regex,
                    onValueChange = { value ->
                        onIntent(ExecutionRulesSettingsIntent.RegexChanged(value))
                    },
                    placeholder = stringResource(R.string.execution_rules_field_regex_hint),
                    enabled = rule.matcherType == ExecutionRuleMatcherType.Regex,
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        actions = {
            DialogActionButton(
                text = stringResource(R.string.execution_rules_cancel_action),
                onClick = {
                    onIntent(ExecutionRulesSettingsIntent.DismissEditingRule)
                },
                primary = false,
            )
            DialogActionButton(
                text = stringResource(R.string.execution_rules_save_action),
                onClick = {
                    onIntent(ExecutionRulesSettingsIntent.SaveEditingRule)
                },
                primary = true,
            )
        },
    )
}

@Composable
private fun ExecutionRuleModeSegmentedControl(
    selected: ExecutionRuleMode,
    onSelected: (ExecutionRuleMode) -> Unit,
) {
    SegmentedControl(
        leftText = stringResource(R.string.execution_rules_mode_blacklist),
        rightText = stringResource(R.string.execution_rules_mode_whitelist),
        leftSelected = selected == ExecutionRuleMode.Blacklist,
        onLeftClick = {
            onSelected(ExecutionRuleMode.Blacklist)
        },
        onRightClick = {
            onSelected(ExecutionRuleMode.Whitelist)
        },
    )
}

@Composable
private fun ExecutionRuleMatcherSegmentedControl(
    selected: ExecutionRuleMatcherType,
    onSelected: (ExecutionRuleMatcherType) -> Unit,
) {
    SegmentedControl(
        leftText = stringResource(R.string.execution_rules_matcher_keywords),
        rightText = stringResource(R.string.execution_rules_matcher_regex),
        leftSelected = selected == ExecutionRuleMatcherType.Keywords,
        onLeftClick = {
            onSelected(ExecutionRuleMatcherType.Keywords)
        },
        onRightClick = {
            onSelected(ExecutionRuleMatcherType.Regex)
        },
    )
}

@Composable
private fun SegmentedControl(
    leftText: String,
    rightText: String,
    leftSelected: Boolean,
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.72f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SegmentedControlItem(
            text = leftText,
            selected = leftSelected,
            onClick = onLeftClick,
        )
        SegmentedControlItem(
            text = rightText,
            selected = !leftSelected,
            onClick = onRightClick,
        )
    }
}

@Composable
private fun RowScope.SegmentedControlItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val background = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0f)
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(20.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ExecutionRuleModeBadge(mode: ExecutionRuleMode) {
    val text = when (mode) {
        ExecutionRuleMode.Blacklist -> stringResource(R.string.execution_rules_mode_blacklist)
        ExecutionRuleMode.Whitelist -> stringResource(R.string.execution_rules_mode_whitelist)
    }
    val color = when (mode) {
        ExecutionRuleMode.Blacklist -> MaterialTheme.colorScheme.errorContainer
        ExecutionRuleMode.Whitelist -> MaterialTheme.colorScheme.primaryContainer
    }
    val contentColor = when (mode) {
        ExecutionRuleMode.Blacklist -> MaterialTheme.colorScheme.onErrorContainer
        ExecutionRuleMode.Whitelist -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            maxLines = 1,
        )
    }
}

@Composable
private fun ExecutionRulesFieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun RowScope.DialogActionButton(
    text: String,
    onClick: () -> Unit,
    primary: Boolean,
) {
    MaterialTintLiquidButton(
        text = text,
        onClick = onClick,
        containerColor = if (primary) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        contentColor = if (primary) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        modifier = Modifier.weight(1f),
    )
}

@Composable
private fun ExecutionRulesListMessage(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}
