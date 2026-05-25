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
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsGroupCard
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsToggleRow
import com.niki914.nexus.agentic.chat.agentic.buildin.BuiltinToolSettingItem
import com.niki914.nexus.agentic.chat.agentic.buildin.BuiltinToolSettingsManager
import com.niki914.nexus.agentic.mod.XService
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun BuiltinToolsSettingsContent(
    topPadding: Dp,
    hazeState: HazeState,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val manager = remember { BuiltinToolSettingsManager() }

    var items by remember { mutableStateOf<List<BuiltinToolSettingItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var savingToolName by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        runCatching {
            loadBuiltinToolItems(context, manager)
        }.onSuccess { loadedItems ->
            items = loadedItems
        }.onFailure { throwable ->
            if (throwable is CancellationException) {
                throw throwable
            }
            statusMessage = context.getString(
                R.string.nexus_builtin_tools_save_failed,
                throwable.message ?: throwable::class.java.simpleName,
            )
        }
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
            text = stringResource(R.string.nexus_settings_builtin_tools),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.nexus_builtin_tools_page_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SettingsGroupCard(title = stringResource(R.string.nexus_builtin_tools_list_title)) {
            when {
                isLoading -> BuiltinToolMessage(
                    text = stringResource(R.string.nexus_builtin_tools_loading),
                )

                items.isEmpty() -> BuiltinToolMessage(
                    text = stringResource(R.string.nexus_builtin_tools_empty),
                )

                else -> items.forEachIndexed { index, item ->
                    SettingsToggleRow(
                        label = item.name,
                        description = item.description,
                        checked = item.enabled,
                        enabled = savingToolName == null,
                        onCheckedChange = { checked ->
                            val previousItems = items
                            items = items.map { current ->
                                if (current.name == item.name) {
                                    current.copy(enabled = checked)
                                } else {
                                    current
                                }
                            }
                            statusMessage = null
                            savingToolName = item.name
                            scope.launch {
                                runCatching {
                                    val result = manager.setEnabled(context, item.name, checked)
                                    if (result.ok) {
                                        items = loadBuiltinToolItems(context, manager)
                                        statusMessage = context.getString(
                                            if (checked) {
                                                R.string.nexus_builtin_tools_save_success_enabled
                                            } else {
                                                R.string.nexus_builtin_tools_save_success_disabled
                                            }
                                        )
                                    } else {
                                        items = refreshBuiltinToolItemsOrFallback(
                                            context = context,
                                            manager = manager,
                                            fallback = previousItems,
                                        )
                                        statusMessage = context.getString(
                                            R.string.nexus_builtin_tools_save_failed,
                                            result.message,
                                        )
                                    }
                                }.onFailure { throwable ->
                                    if (throwable is CancellationException) {
                                        throw throwable
                                    }
                                    items = refreshBuiltinToolItemsOrFallback(
                                        context = context,
                                        manager = manager,
                                        fallback = previousItems,
                                    )
                                    statusMessage = context.getString(
                                        R.string.nexus_builtin_tools_save_failed,
                                        throwable.message ?: throwable::class.java.simpleName,
                                    )
                                }
                                savingToolName = null
                            }
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

        statusMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.nexus_builtin_tools_effect_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BuiltinToolMessage(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
    )
}

private suspend fun loadBuiltinToolItems(
    context: Context,
    manager: BuiltinToolSettingsManager,
): List<BuiltinToolSettingItem> {
    return manager.list(XService.getLocalSettings(context))
}

private suspend fun refreshBuiltinToolItemsOrFallback(
    context: Context,
    manager: BuiltinToolSettingsManager,
    fallback: List<BuiltinToolSettingItem>,
): List<BuiltinToolSettingItem> {
    return runCatching {
        loadBuiltinToolItems(context, manager)
    }.getOrElse { throwable ->
        if (throwable is CancellationException) {
            throw throwable
        }
        fallback
    }
}
