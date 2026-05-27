package com.niki914.nexus.agentic.app.ui.nexus.content

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.niki914.nexus.agentic.app.R
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsGroupCard
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsListPageContent
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsToggleListItemCard
import com.niki914.nexus.agentic.chat.agentic.buildin.BuiltinToolResult
import com.niki914.nexus.agentic.chat.agentic.custom.CustomToolConfig
import com.niki914.nexus.agentic.chat.agentic.custom.CustomToolManager
import com.niki914.nexus.agentic.mod.LocalSettings
import com.niki914.nexus.agentic.mod.XService
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun CustomToolsSettingsContent(
    topPadding: Dp,
    hazeState: HazeState,
    onOpenToolDetail: (toolName: String, toolIndex: Int) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var items by remember { mutableStateOf<List<CustomToolItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val saveFailedTemplate = stringResource(R.string.custom_tool_save_failed)

    LaunchedEffect(Unit) {
        val settings = XService.getLocalSettings(context)
        items = parseCustomToolItems(settings)
        isLoading = false
    }
    val pageDescription = when {
        isLoading || items.isNotEmpty() -> stringResource(R.string.custom_tool_page_description)
        else -> stringResource(R.string.custom_tool_page_empty_description)
    }

    SettingsListPageContent(
        topPadding = topPadding,
        hazeState = hazeState,
        description = pageDescription,
    ) {
        if (isLoading) {
            SettingsGroupCard {
                Text(
                    text = stringResource(R.string.custom_tool_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else if (items.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items.forEachIndexed { index, item ->
                    SettingsToggleListItemCard(
                        title = item.name,
                        checked = item.enabled,
                        enabled = !isSaving,
                        onCheckedChange = { enabled ->
                            val updatedItems = items.toMutableList().also { mutableItems ->
                                mutableItems[index] = item.copy(enabled = enabled)
                            }
                            scope.launch {
                                isSaving = true
                                runCatching {
                                    saveCustomTools(context, updatedItems)
                                }.onSuccess { result ->
                                    if (result.ok) {
                                        items = updatedItems
                                        statusMessage = null
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
                        onClick = {
                            onOpenToolDetail(item.name, index)
                        },
                    )
                }
            }
        }

        statusMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

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
