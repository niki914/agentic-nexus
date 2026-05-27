package com.niki914.nexus.agentic.app.ui.nexus.content

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.niki914.nexus.agentic.app.R
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsGroupCard
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsListPageContent
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsToggleListItemCard
import com.niki914.nexus.agentic.repo.CustomTool
import com.niki914.nexus.agentic.repo.XRepo
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch

@Composable
fun CustomToolsSettingsContent(
    topPadding: Dp,
    hazeState: HazeState,
    onOpenToolDetail: (toolName: String, toolIndex: Int) -> Unit,
) {
    val scope = rememberCoroutineScope()

    var items by remember { mutableStateOf<List<CustomToolItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val saveFailedTemplate = stringResource(R.string.custom_tool_save_failed)

    LaunchedEffect(Unit) {
        items = XRepo.customTools.list().map { it.toItem() }
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
                                    XRepo.customTools.setEnabled(item.name, enabled)
                                }.onSuccess {
                                    items = updatedItems
                                    statusMessage = null
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

private fun CustomTool.toItem(): CustomToolItem {
    return CustomToolItem(
        name = name,
        description = description,
        enabled = enabled,
        command = command,
    )
}
