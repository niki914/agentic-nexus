package com.niki914.nexus.agentic.app.ui.nexus.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.niki914.nexus.agentic.app.R
import com.niki914.nexus.agentic.app.ui.infra.ProvideLiquidScreenContentForPreview
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsGroupCard
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsListItem
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsListPageContent
import com.niki914.nexus.agentic.app.ui.infra.nav.pageViewModel
import com.niki914.nexus.agentic.app.ui.nexus.PageChromeContribution
import com.niki914.nexus.agentic.app.ui.nexus.RegisterPageChrome
import com.niki914.nexus.agentic.app.ui.nexus.model.ExecutionRuleItem
import com.niki914.nexus.agentic.app.ui.nexus.model.ExecutionRulesSettingsIntent
import com.niki914.nexus.agentic.app.ui.nexus.model.ExecutionRulesSettingsUiState
import com.niki914.nexus.agentic.app.ui.nexus.model.ExecutionRulesSettingsViewModel
import com.niki914.nexus.agentic.app.ui.nexus.nav.TopBarActionSpec
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeExecutionRuleEnabledMode

@Composable
fun ExecutionRulesSettingsContent(
    onOpenRuleDetail: (ruleName: String, ruleIndex: Int, isCreating: Boolean) -> Unit,
) {
    val viewModel = pageViewModel<ExecutionRulesSettingsViewModel>()
    val uiState by viewModel.uiStateFlow.collectAsState()
    val createTitle = stringResource(R.string.execution_rules_editor_title_create)
    val latestOnOpenRuleDetail by rememberUpdatedState(onOpenRuleDetail)
    val pageChromeContribution = remember(createTitle) {
        PageChromeContribution(
            rightAction = TopBarActionSpec(
                icon = Icons.Default.Add,
                onClick = {
                    latestOnOpenRuleDetail(createTitle, -1, true)
                },
                contentDescription = createTitle,
            ),
        )
    }
    RegisterPageChrome(pageChromeContribution)

    LaunchedEffect(Unit) {
        viewModel.sendIntent(ExecutionRulesSettingsIntent.Load)
    }

    ExecutionRulesSettingsContentBody(
        uiState = uiState,
        onOpenRuleDetail = onOpenRuleDetail,
    )
}

@Composable
private fun ExecutionRulesSettingsContentBody(
    uiState: ExecutionRulesSettingsUiState,
    onOpenRuleDetail: (ruleName: String, ruleIndex: Int, isCreating: Boolean) -> Unit,
) {

    val pageDescription = when {
        uiState.isLoading || uiState.items.isNotEmpty() -> {
            stringResource(R.string.execution_rules_page_description)
        }

        else -> stringResource(R.string.execution_rules_empty_action_hint)
    }

    SettingsListPageContent(
        description = pageDescription,
    ) {
        if (uiState.isLoading) {
            SettingsGroupCard {
                ExecutionRulesListMessage(text = stringResource(R.string.execution_rules_loading))
            }
        } else if (uiState.items.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                uiState.items.forEachIndexed { index, item ->
                    SettingsGroupCard {
                        SettingsListItem(
                            title = item.name,
                            enabled = !uiState.isSaving,
                            showChevron = true,
                            onClick = {
                                onOpenRuleDetail(item.name, index, false)
                            },
                        )
                    }
                }
            }
        }
    }
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

@Preview(showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun ExecutionRulesSettingsContentPreview() {
    MaterialTheme {
        ProvideLiquidScreenContentForPreview(topPadding = 0.dp) {
            ExecutionRulesSettingsContentBody(
                uiState = ExecutionRulesSettingsUiState(
                    items = listOf(
                        ExecutionRuleItem(
                            id = "builtin-dangerous-delete",
                            name = "危险删改",
                            enabledMode = RuntimeExecutionRuleEnabledMode.LOCKED_ONLY,
                            patterns = listOf("\\brm\\s+-rf\\b"),
                        ),
                        ExecutionRuleItem(
                            id = "builtin-uninstall",
                            name = "卸载相关",
                            enabledMode = RuntimeExecutionRuleEnabledMode.ALWAYS,
                            patterns = listOf("\\bpm\\s+uninstall\\b"),
                        ),
                        ExecutionRuleItem(
                            id = "builtin-privileged",
                            name = "高危提权",
                            enabledMode = RuntimeExecutionRuleEnabledMode.DISABLED,
                            patterns = listOf("\\bsu\\b"),
                        ),
                    ),
                    isLoading = false,
                    isSaving = false,
                ),
                onOpenRuleDetail = { _, _, _ -> },
            )
        }
    }
}
