package com.niki914.nexus.agentic.app.ui.nexus.content

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.niki914.nexus.agentic.app.R
import com.niki914.nexus.agentic.app.ui.infra.component.LiquidTextField
import com.niki914.nexus.agentic.app.ui.infra.component.MaterialTintLiquidButton
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsGroupCard
import com.niki914.nexus.agentic.app.ui.infra.component.SettingNavigationItem
import com.niki914.nexus.agentic.app.ui.infra.component.SettingToggleItem
import com.niki914.nexus.agentic.app.ui.infra.nav.pageViewModel
import com.niki914.nexus.agentic.app.ui.nexus.model.McpSettingsIntent
import com.niki914.nexus.agentic.app.ui.nexus.model.McpSettingsViewModel
import com.niki914.nexus.agentic.mod.XService
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@Composable
fun McpSettingsContent(
    topPadding: Dp,
    hazeState: HazeState,
) {
    val context = LocalContext.current.applicationContext
    val factory = remember(context) { createMcpSettingsViewModelFactory(context) }
    val viewModel = pageViewModel<McpSettingsViewModel>(factory = factory)
    val uiState by viewModel.uiStateFlow.collectAsState()
    val scrollState = rememberScrollState()

    val enabledStateText = stringResource(R.string.mcp_state_enabled)
    val disabledStateText = stringResource(R.string.mcp_state_disabled)
    val summaryFallback = stringResource(R.string.mcp_summary_fallback)
    val duplicateNameError = stringResource(R.string.mcp_duplicate_name)
    val trimmedName = uiState.formState.name.trim()
    val trimmedUrl = uiState.formState.url.trim()
    val hasDuplicateName = trimmedName.isNotBlank() && uiState.items.anyIndexed { index, item ->
        item.name == trimmedName && index != uiState.formState.editingIndex
    }

    LaunchedEffect(Unit) {
        viewModel.sendIntent(McpSettingsIntent.Load)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .hazeSource(hazeState)
            .verticalScroll(scrollState)
            .padding(top = topPadding)
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = stringResource(R.string.ui_settings_mcp),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.mcp_page_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SettingsGroupCard(title = stringResource(R.string.mcp_list_title)) {
            if (uiState.isLoading) {
                Text(
                    text = stringResource(R.string.mcp_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
                )
            } else if (uiState.items.isEmpty()) {
                Text(
                    text = stringResource(R.string.mcp_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
                )
            } else {
                uiState.items.forEachIndexed { index, item ->
                    SettingNavigationItem(
                        title = item.name,
                        summary = item.url.ifBlank { summaryFallback },
                        currentState = if (item.enabled) enabledStateText else disabledStateText,
                        onClick = {
                            viewModel.sendIntent(McpSettingsIntent.StartEdit(index))
                        },
                    )
                    if (index != uiState.items.lastIndex) {
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                    }
                }
            }
        }

        MaterialTintLiquidButton(
            text = stringResource(R.string.mcp_add_action),
            onClick = { viewModel.sendIntent(McpSettingsIntent.StartCreate) },
        )

        SettingsGroupCard(
            title = stringResource(
                if (uiState.formState.editingIndex == null) {
                    R.string.mcp_editor_title_create
                } else {
                    R.string.mcp_editor_title_edit
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SettingsEditorField(label = stringResource(R.string.mcp_field_name)) {
                    LiquidTextField(
                        value = uiState.formState.name,
                        onValueChange = { viewModel.sendIntent(McpSettingsIntent.NameChanged(it)) },
                        placeholder = stringResource(R.string.mcp_field_name_placeholder),
                        singleLine = true,
                    )
                }
                SettingsEditorField(label = stringResource(R.string.mcp_field_url)) {
                    LiquidTextField(
                        value = uiState.formState.url,
                        onValueChange = { viewModel.sendIntent(McpSettingsIntent.UrlChanged(it)) },
                        placeholder = stringResource(R.string.mcp_field_url_placeholder),
                        singleLine = false,
                        minLines = 3,
                    )
                }
                SettingToggleItem(
                    title = stringResource(R.string.mcp_field_enabled),
                    description = stringResource(R.string.mcp_field_enabled_description),
                    checked = uiState.formState.enabled,
                    onCheckedChange = { viewModel.sendIntent(McpSettingsIntent.EnabledChanged(it)) },
                )
                uiState.statusMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                MaterialTintLiquidButton(
                    text = stringResource(R.string.mcp_save_action),
                    enabled = !uiState.isSaving &&
                        trimmedName.isNotBlank() &&
                        trimmedUrl.isNotBlank() &&
                        !hasDuplicateName,
                    onClick = { viewModel.sendIntent(McpSettingsIntent.Save) },
                )
                if (hasDuplicateName) {
                    Text(
                        text = duplicateNameError,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (uiState.formState.editingIndex != null) {
                    MaterialTintLiquidButton(
                        text = stringResource(R.string.mcp_delete_action),
                        enabled = !uiState.isSaving,
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                        onClick = { viewModel.sendIntent(McpSettingsIntent.DeleteCurrent) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsEditorField(
    label: String,
    content: @Composable () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

private fun createMcpSettingsViewModelFactory(
    context: Context,
): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass == McpSettingsViewModel::class.java)
            return McpSettingsViewModel(
                loadSettings = { XService.getLocalSettings(context) },
                saveSettings = { settings -> XService.putLocalSettings(context, settings) },
            ) as T
        }
    }
}

private inline fun <T> List<T>.anyIndexed(predicate: (Int, T) -> Boolean): Boolean {
    forEachIndexed { index, item ->
        if (predicate(index, item)) {
            return true
        }
    }
    return false
}
