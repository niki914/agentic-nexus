package com.niki914.nexus.agentic.app.ui.nexus.content

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.niki914.nexus.agentic.app.R
import com.niki914.nexus.agentic.app.ui.infra.ProvideLiquidScreenContentForPreview
import com.niki914.nexus.agentic.app.ui.infra.component.settings.SettingsPageSpec
import com.niki914.nexus.agentic.app.ui.infra.component.settings.SettingsRowAction
import com.niki914.nexus.agentic.app.ui.infra.component.settings.SettingsRowSpec
import com.niki914.nexus.agentic.app.ui.infra.component.settings.SettingsSectionLayout
import com.niki914.nexus.agentic.app.ui.infra.component.settings.SettingsSectionSpec
import com.niki914.nexus.agentic.app.ui.infra.component.settings.SettingsSpecPageContent
import com.niki914.nexus.agentic.app.ui.infra.nav.pageViewModel
import com.niki914.nexus.agentic.app.ui.nexus.model.SkillInlineError
import com.niki914.nexus.agentic.app.ui.nexus.model.SkillListItem
import com.niki914.nexus.agentic.app.ui.nexus.model.SkillSettingsIntent
import com.niki914.nexus.agentic.app.ui.nexus.model.SkillSettingsUiState
import com.niki914.nexus.agentic.app.ui.nexus.model.SkillSettingsViewModel

@Composable
fun SkillsSettingsContent(
    onOpenSkillDetail: (skillId: String, title: String) -> Unit,
) {
    val viewModel = pageViewModel<SkillSettingsViewModel>()
    val uiState by viewModel.uiStateFlow.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.sendIntent(SkillSettingsIntent.Load)
    }

    SkillsSettingsContentBody(
        uiState = uiState,
        onOpenSkillDetail = onOpenSkillDetail,
        onEnabledChange = { id, enabled ->
            viewModel.sendIntent(SkillSettingsIntent.ToggleEnabled(id, enabled))
        },
    )
}

@Composable
private fun SkillsSettingsContentBody(
    uiState: SkillSettingsUiState,
    onOpenSkillDetail: (String, String) -> Unit,
    onEnabledChange: (String, Boolean) -> Unit,
) {
    val items = uiState.items
    SettingsSpecPageContent(
        spec = skillSettingsSpec(uiState),
        onAction = { action ->
            when (action) {
                is SettingsRowAction.Navigate -> {
                    val item = items.firstOrNull { it.id == action.id } ?: return@SettingsSpecPageContent
                    onOpenSkillDetail(item.id, item.title)
                }

                is SettingsRowAction.ToggleChanged -> onEnabledChange(action.id, action.checked)
                is SettingsRowAction.Click -> Unit
            }
        },
    )
}

@Composable
private fun skillSettingsSpec(uiState: SkillSettingsUiState): SettingsPageSpec {
    val sections = when {
        uiState.isLoading -> listOf(
            SettingsSectionSpec(
                layout = SettingsSectionLayout.GroupedCard,
                rows = listOf(
                    SettingsRowSpec.Message(
                        title = stringResource(R.string.skill_loading),
                        horizontalPadding = 0.dp,
                        verticalPadding = 0.dp,
                    )
                ),
            )
        )

        uiState.items.isNotEmpty() -> listOf(
            SettingsSectionSpec(
                layout = SettingsSectionLayout.CardList,
                rows = uiState.items.map { item ->
                    SettingsRowSpec.ToggleNavigation(
                        id = item.id,
                        title = item.title,
                        summary = item.summary,
                        checked = item.enabled,
                        enabled = !uiState.isSaving,
                    )
                },
            )
        )

        else -> emptyList()
    }

    return SettingsPageSpec(
        description = skillPageDescription(uiState),
        sections = sections,
    )
}

@Composable
private fun skillPageDescription(uiState: SkillSettingsUiState): String {
    return when {
        uiState.inlineError is SkillInlineError.LoadFailed -> stringResource(
            R.string.skill_error_load_failed,
            uiState.inlineError.message,
        )

        uiState.inlineError is SkillInlineError.SaveFailed -> stringResource(
            R.string.skill_error_save_failed,
            uiState.inlineError.message,
        )

        uiState.inlineError is SkillInlineError.DeleteFailed -> stringResource(
            R.string.skill_error_delete_failed,
            uiState.inlineError.message,
        )

        uiState.isLoading -> stringResource(R.string.skill_page_description)
        uiState.items.isEmpty() -> stringResource(R.string.skill_empty)
        else -> stringResource(R.string.skill_page_description)
    }
}

@Preview(name = "Skills Loading", showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun SkillsSettingsContentLoadingPreview() {
    MaterialTheme {
        ProvideLiquidScreenContentForPreview(topPadding = 0.dp) {
            SkillsSettingsContentBody(
                uiState = SkillSettingsUiState(isLoading = true),
                onOpenSkillDetail = { _, _ -> },
                onEnabledChange = { _, _ -> },
            )
        }
    }
}

@Preview(name = "Skills Empty", showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun SkillsSettingsContentEmptyPreview() {
    MaterialTheme {
        ProvideLiquidScreenContentForPreview(topPadding = 0.dp) {
            SkillsSettingsContentBody(
                uiState = SkillSettingsUiState(isLoading = false),
                onOpenSkillDetail = { _, _ -> },
                onEnabledChange = { _, _ -> },
            )
        }
    }
}

@Preview(name = "Skills Loaded", showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun SkillsSettingsContentLoadedPreview() {
    MaterialTheme {
        ProvideLiquidScreenContentForPreview(topPadding = 0.dp) {
            SkillsSettingsContentBody(
                uiState = SkillSettingsUiState(
                    isLoading = false,
                    items = listOf(
                        SkillListItem(
                            id = "tools/android/SKILL.md",
                            title = "Android Debugging",
                            summary = "调试 Android 本地问题",
                            enabled = true,
                        ),
                        SkillListItem(
                            id = "workflow/release/SKILL.md",
                            title = "Release Checklist",
                            summary = "发布前检查",
                            enabled = false,
                        ),
                    ),
                ),
                onOpenSkillDetail = { _, _ -> },
                onEnabledChange = { _, _ -> },
            )
        }
    }
}
