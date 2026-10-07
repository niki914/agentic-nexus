package com.niki914.zafiro.app.ui.content

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.niki914.uikit.infra.ProvideLiquidScreenContentForPreview
import com.niki914.uikit.infra.component.settings.SettingsPageSpec
import com.niki914.uikit.infra.component.settings.SettingsRowAction
import com.niki914.uikit.infra.component.settings.SettingsRowSpec
import com.niki914.uikit.infra.component.settings.SettingsSectionLayout
import com.niki914.uikit.infra.component.settings.SettingsSectionSpec
import com.niki914.uikit.infra.component.settings.SettingsSpecPageContent
import com.niki914.uikit.infra.nav.pageViewModel
import com.niki914.zafiro.app.R
import com.niki914.zafiro.app.ui.PageChromeContribution
import com.niki914.zafiro.app.ui.RegisterPageChrome
import com.niki914.zafiro.app.ui.model.TextActionItem
import com.niki914.zafiro.app.ui.model.TextActionsEffect
import com.niki914.zafiro.app.ui.model.TextActionsIntent
import com.niki914.zafiro.app.ui.model.TextActionsUiState
import com.niki914.zafiro.app.ui.model.TextActionsViewModel
import com.niki914.zafiro.app.ui.nav.TopBarActionSpec

private const val TEXT_ACTION_ROW_ID_PREFIX = "text.action."
private const val SILENT_ROW_ID = "text.action.silent"

@Composable
fun TextActionsSettingsContent(
    onOpenActionDetail: (actionId: String, actionName: String, index: Int, isCreating: Boolean) -> Unit,
) {
    val viewModel = pageViewModel<TextActionsViewModel>()
    val uiState by viewModel.uiStateFlow.collectAsState()
    val context = LocalContext.current
    val createTitle = stringResource(R.string.text_actions_editor_title_create)
    val latestOnOpenActionDetail by rememberUpdatedState(onOpenActionDetail)
    val pageChromeContribution = remember(createTitle) {
        PageChromeContribution(
            rightAction = TopBarActionSpec(
                icon = Icons.Default.Add,
                onClick = {
                    latestOnOpenActionDetail("", createTitle, -1, true)
                },
                contentDescription = createTitle,
            ),
        )
    }
    RegisterPageChrome(pageChromeContribution)

    LaunchedEffect(Unit) {
        viewModel.sendIntent(TextActionsIntent.Load)
    }

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                TextActionsEffect.SilentNeedsCarrier -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.text_actions_silent_needs_carrier),
                        Toast.LENGTH_LONG,
                    ).show()
                }

                TextActionsEffect.ExitDetail,
                TextActionsEffect.FocusName,
                TextActionsEffect.FocusPrompt -> Unit
            }
        }
    }

    TextActionsSettingsContentBody(
        uiState = uiState,
        onOpenActionDetail = onOpenActionDetail,
        onSilentToggle = { viewModel.sendIntent(TextActionsIntent.SilentToggled) },
    )
}

@Composable
private fun TextActionsSettingsContentBody(
    uiState: TextActionsUiState,
    onOpenActionDetail: (actionId: String, actionName: String, index: Int, isCreating: Boolean) -> Unit,
    onSilentToggle: () -> Unit,
) {
    val pageDescription = when {
        uiState.isLoading || uiState.items.isNotEmpty() -> {
            stringResource(R.string.text_actions_page_description)
        }

        else -> stringResource(R.string.text_actions_empty_action_hint)
    }
    val loadingText = stringResource(R.string.text_actions_loading)
    val silentTitle = stringResource(R.string.text_actions_silent)
    val silentSummary = stringResource(R.string.text_actions_silent_summary)

    SettingsSpecPageContent(
        spec = textActionsSettingsSpec(
            uiState = uiState,
            pageDescription = pageDescription,
            loadingText = loadingText,
            silentTitle = silentTitle,
            silentSummary = silentSummary,
        ),
        onAction = { action ->
            when (action) {
                is SettingsRowAction.Navigate -> {
                    val index = textActionIndexFromRowId(action.id) ?: return@SettingsSpecPageContent
                    val item = uiState.items.getOrNull(index) ?: return@SettingsSpecPageContent
                    onOpenActionDetail(item.id, item.name, index, false)
                }

                is SettingsRowAction.ToggleChanged -> {
                    if (action.id == SILENT_ROW_ID) onSilentToggle()
                }

                is SettingsRowAction.Click -> Unit
            }
        },
    )
}

private fun textActionsSettingsSpec(
    uiState: TextActionsUiState,
    pageDescription: String,
    loadingText: String,
    silentTitle: String,
    silentSummary: String,
): SettingsPageSpec {
    val sections = when {
        uiState.isLoading -> listOf(
            SettingsSectionSpec(
                layout = SettingsSectionLayout.GroupedCard,
                rows = listOf(
                    SettingsRowSpec.Message(
                        title = loadingText,
                        verticalPadding = 12.dp,
                    )
                ),
            )
        )

        else -> buildList {
            if (uiState.items.isNotEmpty()) {
                add(
                    SettingsSectionSpec(
                        layout = SettingsSectionLayout.CardList,
                        rows = uiState.items.mapIndexed { index, item ->
                            SettingsRowSpec.Navigation(
                                id = textActionRowId(index),
                                title = item.name,
                                summary = item.promptTemplate,
                                enabled = !uiState.isSaving,
                            )
                        },
                    )
                )
            }
            add(
                SettingsSectionSpec(
                    layout = SettingsSectionLayout.GroupedCard,
                    rows = listOf(
                        SettingsRowSpec.Toggle(
                            id = SILENT_ROW_ID,
                            title = silentTitle,
                            summary = silentSummary,
                            checked = uiState.silentEnabled,
                        ),
                    ),
                )
            )
        }
    }

    return SettingsPageSpec(
        description = pageDescription,
        sections = sections,
    )
}

private fun textActionRowId(index: Int): String = "$TEXT_ACTION_ROW_ID_PREFIX$index"

private fun textActionIndexFromRowId(id: String): Int? {
    if (!id.startsWith(TEXT_ACTION_ROW_ID_PREFIX)) return null
    return id.removePrefix(TEXT_ACTION_ROW_ID_PREFIX).toIntOrNull()
}

@Preview(showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun TextActionsSettingsContentPreview() {
    MaterialTheme {
        ProvideLiquidScreenContentForPreview(topPadding = 0.dp) {
            TextActionsSettingsContentBody(
                uiState = TextActionsUiState(
                    items = listOf(
                        TextActionItem(
                            id = "explain",
                            name = "Explain",
                            promptTemplate = "Explain the following text in simple terms.",
                            enabled = true,
                        ),
                        TextActionItem(
                            id = "translate",
                            name = "Translate",
                            promptTemplate = "Translate the following text into English.",
                            enabled = true,
                        ),
                    ),
                    isLoading = false,
                    isSaving = false,
                ),
                onOpenActionDetail = { _, _, _, _ -> },
                onSilentToggle = {},
            )
        }
    }
}
