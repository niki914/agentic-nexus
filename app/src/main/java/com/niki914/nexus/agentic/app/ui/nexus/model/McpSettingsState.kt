package com.niki914.nexus.agentic.app.ui.nexus.model

import com.niki914.nexus.cb.ComposeMVIViewModel
import com.niki914.nexus.agentic.repo.McpServer
import com.niki914.nexus.agentic.repo.XRepo
import kotlinx.coroutines.CancellationException

data class McpServerItem(
    val name: String,
    val url: String,
    val enabled: Boolean,
    val headers: Map<String, String> = emptyMap(),
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
}

sealed interface McpSettingsEffect

class McpSettingsViewModel internal constructor(
    private val listServers: suspend () -> List<McpServer> = { XRepo.mcp.list() },
    private val saveServer: suspend (McpServer) -> Unit = { server -> XRepo.mcp.save(server) },
    private val replaceServer: suspend (String?, McpServer) -> Unit = { previousName, server ->
        XRepo.mcp.replace(previousName, server)
    },
    private val deleteServer: suspend (String) -> Unit = { name -> XRepo.mcp.delete(name) },
    private val setServerEnabled: suspend (String, Boolean) -> Unit = { name, enabled ->
        XRepo.mcp.setEnabled(name, enabled)
    },
) : ComposeMVIViewModel<McpSettingsIntent, McpSettingsUiState, McpSettingsEffect>() {

    override fun initUiState(): McpSettingsUiState = McpSettingsUiState()

    override suspend fun handleIntent(intent: McpSettingsIntent) {
        when (intent) {
            McpSettingsIntent.Load -> load()
            McpSettingsIntent.StartCreate -> updateState {
                copy(formState = McpServerFormState())
            }
            is McpSettingsIntent.StartEdit -> startEdit(intent.index)
            is McpSettingsIntent.ItemEnabledChanged -> toggleItemEnabled(
                index = intent.index,
                enabled = intent.value,
            )
            is McpSettingsIntent.NameChanged -> updateState {
                copy(formState = formState.copy(name = intent.value))
            }
            is McpSettingsIntent.UrlChanged -> updateState {
                copy(formState = formState.copy(url = intent.value))
            }
            is McpSettingsIntent.EnabledChanged -> updateState {
                copy(formState = formState.copy(enabled = intent.value))
            }
            McpSettingsIntent.Save -> save()
            McpSettingsIntent.DeleteCurrent -> deleteCurrent()
        }
    }

    private suspend fun load() {
        updateState { copy(isLoading = true) }
        try {
            val loadedItems = listServers().map { it.toItem() }
            updateState {
                copy(
                    items = loadedItems,
                    isLoading = false,
                )
            }
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            updateState {
                copy(
                    isLoading = false,
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
            )
        }
        try {
            setServerEnabled(currentItem.name, enabled)
            updateState {
                copy(
                    isSaving = false,
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
                )
            }
        }
    }

    private suspend fun save() {
        val trimmedName = currentState.formState.name.trim()
        val trimmedUrl = currentState.formState.url.trim()
        val editingIndex = currentState.formState.editingIndex
        if (trimmedName.isBlank()) return
        if (trimmedUrl.isBlank()) return
        val hasDuplicateName = currentState.items.anyIndexed { index, item ->
            item.name == trimmedName && index != editingIndex
        }
        if (hasDuplicateName) return

        updateState { copy(isSaving = true) }
        try {
            val previousName = editingIndex
                ?.let { currentState.items.getOrNull(it)?.name }
            val nextItem = McpServerItem(
                name = trimmedName,
                url = trimmedUrl,
                enabled = currentState.formState.enabled,
                headers = editingIndex
                    ?.let { currentState.items.getOrNull(it)?.headers }
                    ?: emptyMap(),
            )
            val updatedItems = currentState.items.toMutableList().also { mutableItems ->
                if (editingIndex == null || editingIndex !in mutableItems.indices) {
                    mutableItems += nextItem
                } else {
                    mutableItems[editingIndex] = nextItem
                }
            }
            if (previousName != null && previousName != nextItem.name) {
                replaceServer(previousName, nextItem.toRepo())
            } else {
                saveServer(nextItem.toRepo())
            }
            updateState {
                copy(
                    items = updatedItems,
                    formState = formState.copy(editingIndex = updatedItems.indexOf(nextItem)),
                    isSaving = false,
                )
            }
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            updateState {
                copy(
                    isSaving = false,
                )
            }
        }
    }

    private suspend fun deleteCurrent() {
        val editingIndex = currentState.formState.editingIndex ?: return
        updateState { copy(isSaving = true) }
        try {
            val updatedItems = currentState.items.filterIndexed { index, _ -> index != editingIndex }
            deleteServer(currentState.items[editingIndex].name)
            updateState {
                copy(
                    items = updatedItems,
                    formState = McpServerFormState(),
                    isSaving = false,
                )
            }
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            updateState {
                copy(
                    isSaving = false,
                )
            }
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

private fun McpServer.toItem(): McpServerItem {
    return McpServerItem(
        name = name,
        url = url,
        enabled = enabled,
        headers = headers,
    )
}

private fun McpServerItem.toRepo(): McpServer {
    return McpServer(
        name = name,
        url = url,
        enabled = enabled,
        headers = headers,
    )
}
