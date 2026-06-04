package com.niki914.nexus.agentic.app.ui.nexus.model

import androidx.lifecycle.viewModelScope
import com.niki914.nexus.agentic.repo.XRepo
import com.niki914.nexus.cb.ComposeMVIViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

data class MemorySettingsUiState(
    val items: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val editingDialog: MemoryEditDialogState? = null,
    val inlineError: MemoryInlineError? = null,
)

data class MemoryEditDialogState(
    val index: Int?,
    val value: String,
)

sealed interface MemoryInlineError {
    data class LoadFailed(val message: String) : MemoryInlineError
    data class SaveFailed(val message: String) : MemoryInlineError
    data class DeleteFailed(val message: String) : MemoryInlineError
}

sealed interface MemorySettingsIntent {
    data object Load : MemorySettingsIntent
    data object StartCreate : MemorySettingsIntent
    data class StartEdit(val index: Int) : MemorySettingsIntent
    data class EditValueChanged(val value: String) : MemorySettingsIntent
    data object DismissEditDialog : MemorySettingsIntent
    data object SaveEditDialog : MemorySettingsIntent
    data class DeleteItem(val index: Int) : MemorySettingsIntent
}

class MemorySettingsViewModel :
    ComposeMVIViewModel<MemorySettingsIntent, MemorySettingsUiState, Nothing>() {

    init {
        viewModelScope.launch {
            settingsChanges.collect {
                load()
            }
        }
    }

    override fun initUiState(): MemorySettingsUiState = MemorySettingsUiState()

    override suspend fun handleIntent(intent: MemorySettingsIntent) {
        when (intent) {
            MemorySettingsIntent.Load -> load()
            MemorySettingsIntent.StartCreate -> startCreate()
            is MemorySettingsIntent.StartEdit -> startEdit(intent.index)
            is MemorySettingsIntent.EditValueChanged -> updateEditValue(intent.value)
            MemorySettingsIntent.DismissEditDialog -> updateState {
                copy(editingDialog = null, inlineError = null)
            }

            MemorySettingsIntent.SaveEditDialog -> saveEditDialog()
            is MemorySettingsIntent.DeleteItem -> deleteItem(intent.index)
        }
    }

    private suspend fun load() {
        updateState { copy(isLoading = true) }
        try {
            val loadedItems = XRepo.memory.list()
            updateState {
                copy(
                    items = loadedItems,
                    isLoading = false,
                    inlineError = null,
                )
            }
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            updateState {
                copy(
                    isLoading = false,
                    inlineError = MemoryInlineError.LoadFailed(
                        throwable.message ?: "读取记忆失败",
                    ),
                )
            }
        }
    }

    private fun startCreate() {
        updateState {
            copy(
                editingDialog = MemoryEditDialogState(index = null, value = ""),
                inlineError = null,
            )
        }
    }

    private fun startEdit(index: Int) {
        val value = currentState.items.getOrNull(index) ?: return
        updateState {
            copy(
                editingDialog = MemoryEditDialogState(index = index, value = value),
                inlineError = null,
            )
        }
    }

    private fun updateEditValue(value: String) {
        val editingDialog = currentState.editingDialog ?: return
        updateState {
            copy(
                editingDialog = editingDialog.copy(value = value),
                inlineError = null,
            )
        }
    }

    private suspend fun saveEditDialog() {
        val dialog = currentState.editingDialog ?: return
        val trimmedValue = dialog.value.trim()
        if (trimmedValue.isBlank()) {
            updateState {
                copy(
                    editingDialog = null,
                    inlineError = null,
                )
            }
            return
        }

        updateState { copy(isSaving = true, inlineError = null) }
        try {
            val updatedItems = currentState.items.toMutableList().also { mutableItems ->
                val index = dialog.index
                if (index == null || index !in mutableItems.indices) {
                    mutableItems += trimmedValue
                } else {
                    mutableItems[index] = trimmedValue
                }
            }
            if (dialog.index == null) {
                XRepo.memory.add(trimmedValue)
            } else {
                XRepo.memory.update(dialog.index, trimmedValue)
            }
            updateState {
                copy(
                    items = updatedItems,
                    isSaving = false,
                    editingDialog = null,
                    inlineError = null,
                )
            }
            notifySettingsChanged()
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            updateState {
                copy(
                    isSaving = false,
                    inlineError = MemoryInlineError.SaveFailed(
                        throwable.message ?: "保存记忆失败",
                    ),
                )
            }
        }
    }

    private suspend fun deleteItem(index: Int) {
        val previousItems = currentState.items
        if (index !in previousItems.indices) return
        updateState { copy(isSaving = true, inlineError = null) }
        try {
            XRepo.memory.delete(index)
            updateState {
                copy(
                    items = previousItems.filterIndexed { itemIndex, _ -> itemIndex != index },
                    isSaving = false,
                    inlineError = null,
                )
            }
            notifySettingsChanged()
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            val reloadedItems = runCatching { XRepo.memory.list() }.getOrDefault(previousItems)
            updateState {
                copy(
                    items = reloadedItems,
                    isSaving = false,
                    inlineError = MemoryInlineError.DeleteFailed(
                        throwable.message ?: "删除记忆失败",
                    ),
                )
            }
        }
    }

    private fun notifySettingsChanged() {
        settingsChanges.tryEmit(Unit)
    }

    private companion object {
        val settingsChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    }
}
