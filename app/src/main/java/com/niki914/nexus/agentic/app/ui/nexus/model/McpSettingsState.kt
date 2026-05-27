package com.niki914.nexus.agentic.app.ui.nexus.model

import android.content.Context
import com.niki914.nexus.agentic.app.R
import com.niki914.nexus.agentic.mod.LocalSettings
import com.niki914.nexus.cb.ComposeMVIViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

data class McpServerItem(
    val name: String,
    val url: String,
    val enabled: Boolean,
)

data class McpServerFormState(
    val editingIndex: Int? = null,
    val name: String = "",
    val url: String = "",
    val enabled: Boolean = true,
)

data class McpSettingsUiState(
    val items: List<McpServerItem> = emptyList(),
    val formState: McpServerFormState = McpServerFormState(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val statusMessage: String? = null,
)

sealed interface McpSettingsIntent {
    data object Load : McpSettingsIntent
    data object StartCreate : McpSettingsIntent
    data class StartEdit(val index: Int) : McpSettingsIntent
    data class ItemEnabledChanged(val index: Int, val value: Boolean) : McpSettingsIntent
    data class NameChanged(val value: String) : McpSettingsIntent
    data class UrlChanged(val value: String) : McpSettingsIntent
    data class EnabledChanged(val value: Boolean) : McpSettingsIntent
    data object Save : McpSettingsIntent
    data object DeleteCurrent : McpSettingsIntent
    data object DismissStatus : McpSettingsIntent
}

sealed interface McpSettingsEffect

class McpSettingsStrings(
    private val context: Context,
) {
    fun loadFailed(message: String): String = context.getString(R.string.mcp_load_failed, message)
    fun toggleSucceeded(enabled: Boolean): String = context.getString(
        if (enabled) R.string.mcp_save_success_enabled else R.string.mcp_save_success_disabled
    )
    fun saveFailed(message: String): String = context.getString(R.string.mcp_save_failed, message)
    fun nameRequired(): String = context.getString(R.string.mcp_name_required)
    fun urlRequired(): String = context.getString(R.string.mcp_url_required)
    fun duplicateName(): String = context.getString(R.string.mcp_duplicate_name)
    fun saveSucceeded(): String = context.getString(R.string.mcp_save_success)
    fun deleteSucceeded(): String = context.getString(R.string.mcp_delete_success)
}

class McpSettingsViewModel internal constructor(
    private val loadSettings: suspend () -> LocalSettings,
    private val saveSettings: suspend (LocalSettings) -> Unit,
    private val strings: McpSettingsStrings,
) : ComposeMVIViewModel<McpSettingsIntent, McpSettingsUiState, McpSettingsEffect>() {

    override fun initUiState(): McpSettingsUiState = McpSettingsUiState()

    override suspend fun handleIntent(intent: McpSettingsIntent) {
        when (intent) {
            McpSettingsIntent.Load -> load()
            McpSettingsIntent.StartCreate -> updateState {
                copy(
                    formState = McpServerFormState(),
                    statusMessage = null,
                )
            }
            is McpSettingsIntent.StartEdit -> startEdit(intent.index)
            is McpSettingsIntent.ItemEnabledChanged -> toggleItemEnabled(
                index = intent.index,
                enabled = intent.value,
            )
            is McpSettingsIntent.NameChanged -> updateState {
                copy(
                    formState = formState.copy(name = intent.value),
                    statusMessage = null,
                )
            }
            is McpSettingsIntent.UrlChanged -> updateState {
                copy(
                    formState = formState.copy(url = intent.value),
                    statusMessage = null,
                )
            }
            is McpSettingsIntent.EnabledChanged -> updateState {
                copy(
                    formState = formState.copy(enabled = intent.value),
                    statusMessage = null,
                )
            }
            McpSettingsIntent.Save -> save()
            McpSettingsIntent.DeleteCurrent -> deleteCurrent()
            McpSettingsIntent.DismissStatus -> updateState { copy(statusMessage = null) }
        }
    }

    private suspend fun load() {
        updateState { copy(isLoading = true, statusMessage = null) }
        try {
            val settings = loadSettings()
            updateState {
                copy(
                    items = parseMcpServerItems(settings),
                    isLoading = false,
                    statusMessage = null,
                )
            }
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            updateState {
                copy(
                    isLoading = false,
                    statusMessage = strings.loadFailed(throwable.message ?: throwable::class.java.simpleName),
                )
            }
        }
    }

    private fun startEdit(index: Int) {
        val item = currentState.items.getOrNull(index) ?: return
        updateState {
            copy(
                formState = McpServerFormState(
                    editingIndex = index,
                    name = item.name,
                    url = item.url,
                    enabled = item.enabled,
                ),
                statusMessage = null,
            )
        }
    }

    private suspend fun toggleItemEnabled(index: Int, enabled: Boolean) {
        val currentItem = currentState.items.getOrNull(index) ?: return
        val previousItems = currentState.items
        val updatedItems = previousItems.toMutableList().apply {
            this[index] = currentItem.copy(enabled = enabled)
        }
        updateState {
            copy(
                items = updatedItems,
                formState = if (formState.editingIndex == index) {
                    formState.copy(enabled = enabled)
                } else {
                    formState
                },
                isSaving = true,
                statusMessage = null,
            )
        }
        try {
            val latestSettings = loadSettings()
            saveSettings(buildUpdatedLocalSettings(latestSettings, updatedItems))
            updateState {
                copy(
                    isSaving = false,
                    statusMessage = strings.toggleSucceeded(enabled),
                )
            }
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            updateState {
                copy(
                    items = previousItems,
                    formState = if (formState.editingIndex == index) {
                        formState.copy(enabled = previousItems[index].enabled)
                    } else {
                        formState
                    },
                    isSaving = false,
                    statusMessage = strings.saveFailed(throwable.message ?: throwable::class.java.simpleName),
                )
            }
        }
    }

    private suspend fun save() {
        val trimmedName = currentState.formState.name.trim()
        val trimmedUrl = currentState.formState.url.trim()
        val editingIndex = currentState.formState.editingIndex
        if (trimmedName.isBlank()) {
            updateState { copy(statusMessage = strings.nameRequired()) }
            return
        }
        if (trimmedUrl.isBlank()) {
            updateState { copy(statusMessage = strings.urlRequired()) }
            return
        }
        val hasDuplicateName = currentState.items.anyIndexed { index, item ->
            item.name == trimmedName && index != editingIndex
        }
        if (hasDuplicateName) {
            updateState { copy(statusMessage = strings.duplicateName()) }
            return
        }

        updateState { copy(isSaving = true, statusMessage = null) }
        try {
            val nextItem = McpServerItem(
                name = trimmedName,
                url = trimmedUrl,
                enabled = currentState.formState.enabled,
            )
            val updatedItems = currentState.items.toMutableList().also { mutableItems ->
                if (editingIndex == null || editingIndex !in mutableItems.indices) {
                    mutableItems += nextItem
                } else {
                    mutableItems[editingIndex] = nextItem
                }
            }
            val latestSettings = loadSettings()
            saveSettings(buildUpdatedLocalSettings(latestSettings, updatedItems))
            updateState {
                copy(
                    items = updatedItems,
                    formState = formState.copy(editingIndex = updatedItems.indexOf(nextItem)),
                    isSaving = false,
                    statusMessage = strings.saveSucceeded(),
                )
            }
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            updateState {
                copy(
                    isSaving = false,
                    statusMessage = strings.saveFailed(throwable.message ?: throwable::class.java.simpleName),
                )
            }
        }
    }

    private suspend fun deleteCurrent() {
        val editingIndex = currentState.formState.editingIndex ?: return
        updateState { copy(isSaving = true, statusMessage = null) }
        try {
            val updatedItems = currentState.items.filterIndexed { index, _ -> index != editingIndex }
            val latestSettings = loadSettings()
            saveSettings(buildUpdatedLocalSettings(latestSettings, updatedItems))
            updateState {
                copy(
                    items = updatedItems,
                    formState = McpServerFormState(),
                    isSaving = false,
                    statusMessage = strings.deleteSucceeded(),
                )
            }
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            updateState {
                copy(
                    isSaving = false,
                    statusMessage = strings.saveFailed(throwable.message ?: throwable::class.java.simpleName),
                )
            }
        }
    }
}

internal fun parseMcpServerItems(settings: LocalSettings): List<McpServerItem> {
    return settings.mcpServers
        ?.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val name = obj["name"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            val url = obj["url"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            if (name.isBlank() && url.isBlank()) {
                return@mapNotNull null
            }
            McpServerItem(
                name = name,
                url = url,
                enabled = obj["enabled"]?.jsonPrimitive?.booleanOrNull ?: true,
            )
        }
        ?: emptyList()
}

internal fun buildUpdatedLocalSettings(
    settings: LocalSettings,
    items: List<McpServerItem>,
): LocalSettings {
    val updatedProps = settings.props.toMutableMap()
    updatedProps["mcp_servers"] = JsonArray(
        items.map { item ->
            JsonObject(
                mapOf(
                    "name" to JsonPrimitive(item.name),
                    "url" to JsonPrimitive(item.url),
                    "enabled" to JsonPrimitive(item.enabled),
                )
            )
        }
    )
    return LocalSettings(JsonObject(updatedProps))
}

private inline fun <T> List<T>.anyIndexed(predicate: (Int, T) -> Boolean): Boolean {
    forEachIndexed { index, item ->
        if (predicate(index, item)) {
            return true
        }
    }
    return false
}
