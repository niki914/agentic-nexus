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
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsListPageContent
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsToggleListItemCard
import com.niki914.nexus.agentic.app.ui.infra.nav.pageViewModel
import com.niki914.nexus.agentic.app.ui.nexus.PageChromeContribution
import com.niki914.nexus.agentic.app.ui.nexus.RegisterPageChrome
import com.niki914.nexus.agentic.app.ui.nexus.model.TakeoverInlineError
import com.niki914.nexus.agentic.app.ui.nexus.model.TakeoverRuleItem
import com.niki914.nexus.agentic.app.ui.nexus.model.TakeoverSettingsIntent
import com.niki914.nexus.agentic.app.ui.nexus.model.TakeoverSettingsUiState
import com.niki914.nexus.agentic.app.ui.nexus.model.TakeoverSettingsViewModel
import com.niki914.nexus.agentic.app.ui.nexus.model.TakeoverTarget
import com.niki914.nexus.agentic.app.ui.nexus.nav.TopBarActionSpec

@Composable
fun TakeoverSettingsContent(
    onOpenRuleDetail: (ruleId: String?, ruleName: String, ruleIndex: Int, isCreating: Boolean) -> Unit,
) {
    val viewModel = pageViewModel<TakeoverSettingsViewModel>()
    val uiState by viewModel.uiStateFlow.collectAsState()
    val createTitle = stringResource(R.string.takeover_editor_title_create)
    val latestOnOpenRuleDetail by rememberUpdatedState(onOpenRuleDetail)
    val pageChromeContribution = remember(createTitle) {
        PageChromeContribution(
            rightAction = TopBarActionSpec(
                icon = Icons.Default.Add,
                onClick = {
                    latestOnOpenRuleDetail(null, createTitle, -1, true)
                },
                contentDescription = createTitle,
            ),
        )
    }
    RegisterPageChrome(pageChromeContribution)

    LaunchedEffect(Unit) {
        viewModel.sendIntent(TakeoverSettingsIntent.Load)
    }

    TakeoverSettingsContentBody(
        uiState = uiState,
        onOpenRuleDetail = onOpenRuleDetail,
        onItemEnabledChanged = { index, enabled ->
            viewModel.sendIntent(TakeoverSettingsIntent.ItemEnabledChanged(index, enabled))
        },
    )
}

@Composable
private fun TakeoverSettingsContentBody(
    uiState: TakeoverSettingsUiState,
    onOpenRuleDetail: (ruleId: String?, ruleName: String, ruleIndex: Int, isCreating: Boolean) -> Unit,
    onItemEnabledChanged: (index: Int, enabled: Boolean) -> Unit = { _, _ -> },
) {
    val pageDescription = when {
        uiState.isLoading || uiState.items.isNotEmpty() -> {
            stringResource(R.string.takeover_page_description)
        }

        else -> stringResource(R.string.takeover_empty_action_hint)
    }

    SettingsListPageContent(
        description = pageDescription,
    ) {
        takeoverInlineErrorText(uiState.inlineError)?.let { message ->
            SettingsGroupCard {
                TakeoverListMessage(text = message)
            }
        }
        if (uiState.isLoading) {
            SettingsGroupCard {
                TakeoverListMessage(text = stringResource(R.string.takeover_loading))
            }
        } else if (uiState.items.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                uiState.items.forEachIndexed { index, item ->
                    SettingsToggleListItemCard(
                        title = item.name,
                        checked = item.enabled,
                        enabled = !uiState.isSaving,
                        onCheckedChange = { checked ->
                            onItemEnabledChanged(index, checked)
                        },
                        onClick = {
                            onOpenRuleDetail(item.id, item.name, index, false)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TakeoverListMessage(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun takeoverInlineErrorText(error: TakeoverInlineError?): String? {
    return when (error) {
        null -> null
        is TakeoverInlineError.LoadFailed -> takeoverErrorText(
            message = error.causeMessage,
            genericResId = R.string.takeover_error_load_failed_generic,
            detailedResId = R.string.takeover_error_load_failed,
        )

        is TakeoverInlineError.SaveFailed -> takeoverErrorText(
            message = error.causeMessage,
            genericResId = R.string.takeover_error_save_failed_generic,
            detailedResId = R.string.takeover_error_save_failed,
        )

        is TakeoverInlineError.DeleteFailed -> takeoverErrorText(
            message = error.causeMessage,
            genericResId = R.string.takeover_error_delete_failed_generic,
            detailedResId = R.string.takeover_error_delete_failed,
        )
    }
}

@Composable
private fun takeoverErrorText(
    message: String?,
    genericResId: Int,
    detailedResId: Int,
): String {
    return if (message.isNullOrBlank()) {
        stringResource(genericResId)
    } else {
        stringResource(detailedResId, message)
    }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun TakeoverSettingsContentPreview() {
    MaterialTheme {
        ProvideLiquidScreenContentForPreview(topPadding = 0.dp) {
            TakeoverSettingsContentBody(
                uiState = TakeoverSettingsUiState(
                    items = listOf(
                        TakeoverRuleItem(
                            id = "native-weather",
                            name = "小布生活服务",
                            target = TakeoverTarget.NativeAssistant,
                            patterns = listOf("天气", "闹钟", "日程"),
                        ),
                        TakeoverRuleItem(
                            id = "nexus-debug",
                            name = "调试问题交给 Nexus",
                            target = TakeoverTarget.Nexus,
                            patterns = listOf(".*崩溃.*", ".*日志.*"),
                        ),
                        TakeoverRuleItem(
                            id = "native-media",
                            name = "媒体控制回退原生",
                            target = TakeoverTarget.NativeAssistant,
                            patterns = listOf("播放", "暂停"),
                            enabled = false,
                        ),
                    ),
                ),
                onOpenRuleDetail = { _, _, _, _ -> },
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun TakeoverSettingsEmptyContentPreview() {
    MaterialTheme {
        ProvideLiquidScreenContentForPreview(topPadding = 0.dp) {
            TakeoverSettingsContentBody(
                uiState = TakeoverSettingsUiState(),
                onOpenRuleDetail = { _, _, _, _ -> },
            )
        }
    }
}
