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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.niki914.nexus.agentic.app.R
import com.niki914.nexus.agentic.app.ui.infra.component.MaterialTintLiquidButton
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsGroupCard
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsNavigationRow
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsToggleRow
import com.niki914.nexus.agentic.app.ui.infra.component.StyledTextField
import com.niki914.nexus.agentic.chat.agentic.BuiltinToolResult
import com.niki914.nexus.agentic.chat.agentic.CustomToolConfig
import com.niki914.nexus.agentic.chat.agentic.CustomToolManager
import com.niki914.nexus.agentic.mod.LocalSettings
import com.niki914.nexus.agentic.mod.XService
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun CustomToolsSettingsContent(
    topPadding: Dp,
    hazeState: HazeState,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var items by remember { mutableStateOf<List<CustomToolItem>>(emptyList()) }
    var formState by remember { mutableStateOf(CustomToolFormState()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val enabledStateText = stringResource(R.string.nexus_custom_tools_state_enabled)
    val disabledStateText = stringResource(R.string.nexus_custom_tools_state_disabled)
    val summaryFallback = stringResource(R.string.nexus_custom_tools_summary_fallback)
    val duplicateNameError = stringResource(R.string.nexus_custom_tools_duplicate_name)
    val saveSuccessText = stringResource(R.string.nexus_custom_tools_save_success)
    val deleteSuccessText = stringResource(R.string.nexus_custom_tools_delete_success)
    val saveFailedTemplate = stringResource(R.string.nexus_custom_tools_save_failed)
    val trimmedName = formState.name.trim()
    val hasDuplicateName = trimmedName.isNotBlank() && items.anyIndexed { index, item ->
        item.name == trimmedName && index != formState.editingIndex
    }

    LaunchedEffect(Unit) {
        val settings = XService.getLocalSettings(context)
        items = parseCustomToolItems(settings)
        isLoading = false
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
            text = stringResource(R.string.nexus_settings_custom_tools),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.nexus_custom_tools_page_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SettingsGroupCard(title = stringResource(R.string.nexus_custom_tools_list_title)) {
            if (isLoading) {
                Text(
                    text = stringResource(R.string.nexus_custom_tools_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
                )
            } else if (items.isEmpty()) {
                Text(
                    text = stringResource(R.string.nexus_custom_tools_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
                )
            } else {
                items.forEachIndexed { index, item ->
                    SettingsNavigationRow(
                        title = item.name,
                        summary = item.description.ifBlank { item.command.ifBlank { summaryFallback } },
                        currentState = if (item.enabled) enabledStateText else disabledStateText,
                        onClick = {
                            formState = CustomToolFormState(
                                editingIndex = index,
                                name = item.name,
                                description = item.description,
                                enabled = item.enabled,
                                command = item.command,
                            )
                            statusMessage = null
                        },
                    )
                    if (index != items.lastIndex) {
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
            text = stringResource(R.string.nexus_custom_tools_add_action),
            onClick = {
                formState = CustomToolFormState()
                statusMessage = null
            },
        )

        SettingsGroupCard(
            title = stringResource(
                if (formState.editingIndex == null) {
                    R.string.nexus_custom_tools_editor_title_create
                } else {
                    R.string.nexus_custom_tools_editor_title_edit
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                StyledTextField(
                    value = formState.name,
                    onValueChange = { formState = formState.copy(name = it) },
                    label = stringResource(R.string.nexus_custom_tools_field_name),
                    placeholder = stringResource(R.string.nexus_custom_tools_field_name_placeholder),
                )
                StyledTextField(
                    value = formState.description,
                    onValueChange = { formState = formState.copy(description = it) },
                    label = stringResource(R.string.nexus_custom_tools_field_description),
                    placeholder = stringResource(R.string.nexus_custom_tools_field_description_placeholder),
                )
                SettingsToggleRow(
                    label = stringResource(R.string.nexus_custom_tools_field_enabled),
                    description = stringResource(R.string.nexus_custom_tools_field_enabled_description),
                    checked = formState.enabled,
                    onCheckedChange = { formState = formState.copy(enabled = it) },
                )
                StyledTextField(
                    value = formState.command,
                    onValueChange = { formState = formState.copy(command = it) },
                    label = stringResource(R.string.nexus_custom_tools_field_command),
                    placeholder = stringResource(R.string.nexus_custom_tools_field_command_placeholder),
                    singleLine = false,
                    minLines = 4,
                )
                statusMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                MaterialTintLiquidButton(
                    text = stringResource(R.string.nexus_custom_tools_save_action),
                    enabled = !isSaving &&
                        trimmedName.isNotBlank() &&
                        formState.command.trim().isNotBlank() &&
                        !hasDuplicateName,
                    onClick = {
                        scope.launch {
                            isSaving = true
                            val nextItem = CustomToolItem(
                                name = trimmedName,
                                description = formState.description.trim(),
                                enabled = formState.enabled,
                                command = formState.command.trim(),
                            )
                            val updatedItems = items.toMutableList().also { mutableItems ->
                                val editingIndex = formState.editingIndex
                                if (editingIndex == null || editingIndex !in mutableItems.indices) {
                                    mutableItems += nextItem
                                } else {
                                    mutableItems[editingIndex] = nextItem
                                }
                            }
                            runCatching {
                                saveCustomTools(context, updatedItems)
                            }.onSuccess { result ->
                                if (result.ok) {
                                    items = updatedItems
                                    formState = formState.copy(
                                        editingIndex = updatedItems.indexOf(nextItem)
                                    )
                                    statusMessage = saveSuccessText
                                } else {
                                    statusMessage = result.message
                                }
                            }.onFailure { throwable ->
                                statusMessage = saveFailedTemplate.format(
                                    throwable.message ?: throwable::class.java.simpleName
                                )
                            }
                            isSaving = false
                        }
                    },
                )
                if (hasDuplicateName) {
                    Text(
                        text = duplicateNameError,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (formState.editingIndex != null) {
                    MaterialTintLiquidButton(
                        text = stringResource(R.string.nexus_custom_tools_delete_action),
                        enabled = !isSaving,
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                        onClick = {
                            val editingIndex = formState.editingIndex ?: return@MaterialTintLiquidButton
                            scope.launch {
                                isSaving = true
                                val updatedItems = items.filterIndexed { index, _ -> index != editingIndex }
                                runCatching {
                                    saveCustomTools(context, updatedItems)
                                }.onSuccess { result ->
                                    if (result.ok) {
                                        items = updatedItems
                                        formState = CustomToolFormState()
                                        statusMessage = deleteSuccessText
                                    } else {
                                        statusMessage = result.message
                                    }
                                }.onFailure { throwable ->
                                    statusMessage = saveFailedTemplate.format(
                                        throwable.message ?: throwable::class.java.simpleName
                                    )
                                }
                                isSaving = false
                            }
                        },
                    )
                }
            }
        }
    }
}

private data class CustomToolFormState(
    val editingIndex: Int? = null,
    val name: String = "",
    val description: String = "",
    val enabled: Boolean = true,
    val command: String = "",
)

private data class CustomToolItem(
    val name: String,
    val description: String,
    val enabled: Boolean,
    val command: String,
)

private fun parseCustomToolItems(settings: LocalSettings): List<CustomToolItem> {
    return settings.customTools
        ?.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val name = obj["name"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            val command = obj["command"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            if (name.isBlank() && command.isBlank()) {
                return@mapNotNull null
            }
            CustomToolItem(
                name = name,
                description = obj["description"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                enabled = obj["enabled"]?.jsonPrimitive?.booleanOrNull ?: true,
                command = command,
            )
        }
        ?: emptyList()
}

private suspend fun saveCustomTools(
    context: Context,
    items: List<CustomToolItem>,
): BuiltinToolResult {
    return CustomToolManager().saveAll(
        context = context,
        items = items.map { item ->
            CustomToolConfig(
                name = item.name,
                description = item.description,
                enabled = item.enabled,
                command = item.command,
            )
        },
    )
}

private inline fun <T> List<T>.anyIndexed(predicate: (Int, T) -> Boolean): Boolean {
    forEachIndexed { index, item ->
        if (predicate(index, item)) {
            return true
        }
    }
    return false
}
