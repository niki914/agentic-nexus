package com.niki914.nexus.agentic.app.ui.infra.component.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.niki914.nexus.agentic.app.ui.infra.component.SettingNavigationItem
import com.niki914.nexus.agentic.app.ui.infra.component.SettingToggleItem
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsGroupCard
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsItemDivider
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsListItem
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsListPageContent
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsToggleListItemCard

@Composable
fun SettingsSpecPageContent(
    spec: SettingsPageSpec,
    modifier: Modifier = Modifier,
    onAction: (SettingsRowAction) -> Unit,
) {
    SettingsListPageContent(
        description = spec.description,
        modifier = modifier,
    ) {
        spec.sections.forEach { section ->
            SettingsSectionContent(
                section = section,
                onAction = onAction,
            )
        }
    }
}

@Composable
private fun SettingsSectionContent(
    section: SettingsSectionSpec,
    onAction: (SettingsRowAction) -> Unit,
) {
    if (section.rows.isEmpty()) {
        return
    }

    when (section.layout) {
        SettingsSectionLayout.GroupedCard -> {
            SettingsGroupCard(title = section.title) {
                section.rows.forEachIndexed { index, row ->
                    SettingsRowContent(
                        row = row,
                        layout = section.layout,
                        onAction = onAction,
                    )
                    if (index != section.rows.lastIndex) {
                        SettingsItemDivider()
                    }
                }
            }
        }

        SettingsSectionLayout.CardList -> {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                section.rows.forEach { row ->
                    SettingsCardListRowContent(
                        row = row,
                        onAction = onAction,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsCardListRowContent(
    row: SettingsRowSpec,
    onAction: (SettingsRowAction) -> Unit,
) {
    if (row is SettingsRowSpec.ToggleNavigation) {
        SettingsRowContent(
            row = row,
            layout = SettingsSectionLayout.CardList,
            onAction = onAction,
        )
    } else {
        SettingsGroupCard {
            SettingsRowContent(
                row = row,
                layout = SettingsSectionLayout.CardList,
                onAction = onAction,
            )
        }
    }
}

@Composable
private fun SettingsRowContent(
    row: SettingsRowSpec,
    layout: SettingsSectionLayout,
    onAction: (SettingsRowAction) -> Unit,
) {
    when (row) {
        is SettingsRowSpec.Navigation -> SettingNavigationItem(
            title = row.title,
            summary = row.summary,
            currentState = row.currentState,
            enabled = row.enabled,
            onClick = { onAction(SettingsRowAction.Navigate(row.id)) },
        )

        is SettingsRowSpec.Toggle -> SettingToggleItem(
            title = row.title,
            description = row.summary,
            checked = row.checked,
            enabled = row.enabled,
            onCheckedChange = { checked ->
                onAction(SettingsRowAction.ToggleChanged(row.id, checked))
            },
        )

        is SettingsRowSpec.ToggleNavigation -> {
            require(layout == SettingsSectionLayout.CardList) {
                "SettingsRowSpec.ToggleNavigation requires SettingsSectionLayout.CardList."
            }
            SettingsToggleListItemCard(
                title = row.title,
                checked = row.checked,
                enabled = row.enabled,
                onCheckedChange = { checked ->
                    onAction(SettingsRowAction.ToggleChanged(row.id, checked))
                },
                onClick = { onAction(SettingsRowAction.Navigate(row.id)) },
            )
        }

        is SettingsRowSpec.Value -> SettingsListItem(
            title = row.title,
            summary = row.summary,
            currentState = row.currentState,
            enabled = row.enabled,
            onClick = null,
        )

        is SettingsRowSpec.Action -> SettingsListItem(
            title = row.title,
            summary = row.summary,
            enabled = row.enabled,
            onClick = { onAction(SettingsRowAction.Click(row.id)) },
        )

        is SettingsRowSpec.Destructive -> SettingsListItem(
            title = row.title,
            summary = row.summary,
            enabled = row.enabled,
            onClick = { onAction(SettingsRowAction.Click(row.id)) },
        )

        is SettingsRowSpec.Message -> SettingsMessageItem(text = row.title)
    }
}

@Composable
private fun SettingsMessageItem(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 20.dp),
    )
}
