package com.niki914.zafiro.app.ui.model

import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import com.niki914.logging.Logger
import com.niki914.uikit.base.ComposeMVIViewModel
import com.niki914.zafiro.app.R
import com.niki914.zafiro.repo.TextAction
import com.niki914.zafiro.repo.XRepo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class TextActionItem(
    val id: String,
    val name: String,
    val promptTemplate: String,
    val enabled: Boolean,
)

data class TextActionFormState(
    val editingIndex: Int? = null,
    val previousId: String? = null,
    val id: String? = null,
    val name: String = "",
    val promptInput: String = "",
    val initialSnapshot: TextActionFormSnapshot? = null,
    @param:StringRes val nameErrorResId: Int? = null,
    @param:StringRes val promptErrorResId: Int? = null,
)

data class TextActionFormSnapshot(
    val name: String,
    val prompt: String,
)

val TextActionFormState.hasUnsavedChanges: Boolean
    get() = initialSnapshot?.let { it != toSnapshot() } ?: false

data class TextActionDeleteConfirmationState(
    val value: String,
)

data class TextActionsUiState(
    val items: List<TextActionItem> = emptyList(),
    val formState: TextActionFormState = TextActionFormState(),
    /** 全局「后台静默」开关。 */
    val silentEnabled: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val inlineError: TextActionsInlineError? = null,
    val deleteConfirmation: TextActionDeleteConfirmationState? = null,
)

sealed interface TextActionsIntent {
    data object Load : TextActionsIntent
    data object StartCreate : TextActionsIntent
    data class StartEdit(val index: Int) : TextActionsIntent
    data class ItemEnabledChanged(val index: Int, val value: Boolean) : TextActionsIntent
    data class NameChanged(val value: String) : TextActionsIntent
    data class PromptChanged(val value: String) : TextActionsIntent
    data object SilentToggled : TextActionsIntent
    data object Save : TextActionsIntent
    data object RequestDelete : TextActionsIntent
    data object DismissDeleteConfirmation : TextActionsIntent
    data object ConfirmDelete : TextActionsIntent
}

sealed interface TextActionsInlineError {
    data class LoadFailed(val message: String?, @StringRes val fallbackResId: Int) :
        TextActionsInlineError

    data class SaveFailed(val message: String?, @StringRes val fallbackResId: Int) :
        TextActionsInlineError

    data class DeleteFailed(val message: String?, @StringRes val fallbackResId: Int) :
        TextActionsInlineError
}

sealed interface TextActionsEffect {
    /** 静默开关被拒：悬浮球与常驻通知均未启用。 */
    data object SilentNeedsCarrier : TextActionsEffect

    data object ExitDetail : TextActionsEffect

    data object FocusName : TextActionsEffect

    data object FocusPrompt : TextActionsEffect
}

class TextActionsViewModel :
    ComposeMVIViewModel<TextActionsIntent, TextActionsUiState, TextActionsEffect>() {

    init {
        viewModelScope.launch {
            settingsChanges.collect {
                load()
            }
        }
    }

    override fun initUiState(): TextActionsUiState = TextActionsUiState()

    override suspend fun handleIntent(intent: TextActionsIntent) {
        when (intent) {
            TextActionsIntent.Load -> load()
            TextActionsIntent.StartCreate -> startCreate()
            is TextActionsIntent.StartEdit -> startEdit(intent.index)
            is TextActionsIntent.ItemEnabledChanged -> toggleItemEnabled(
                index = intent.index,
                enabled = intent.value,
            )

            is TextActionsIntent.NameChanged -> updateState {
                copy(
                    formState = formState.copy(name = intent.value, nameErrorResId = null),
                    inlineError = null,
                )
            }

            is TextActionsIntent.PromptChanged -> updateState {
                copy(
                    formState = formState.copy(promptInput = intent.value, promptErrorResId = null),
                    inlineError = null,
                )
            }

            TextActionsIntent.SilentToggled -> toggleSilent()
            TextActionsIntent.Save -> save()
            TextActionsIntent.RequestDelete -> requestDelete()
            TextActionsIntent.DismissDeleteConfirmation -> updateState {
                copy(deleteConfirmation = null)
            }

            TextActionsIntent.ConfirmDelete -> confirmDelete()
        }
    }

    private suspend fun load() {
        updateState { copy(isLoading = true) }
        try {
            val loadedItems = XRepo.textActions.list().map { it.toItem() }
            val silent = XRepo.textActionSilent()
            updateState {
                copy(items = loadedItems, silentEnabled = silent, isLoading = false, inlineError = null)
            }
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            Logger.w(LOG_TAG, "load failed reason=${throwable.message}")
            updateState {
                copy(
                    isLoading = false,
                    inlineError = TextActionsInlineError.LoadFailed(
                        message = throwable.message,
                        fallbackResId = R.string.error_text_actions_load_failed,
                    ),
                )
            }
        }
    }

    private fun startCreate() {
        updateState {
            copy(
                formState = TextActionFormState().withCurrentSnapshotAsInitial(),
                inlineError = null,
            )
        }
    }

    private fun startEdit(index: Int) {
        val item = currentState.items.getOrNull(index) ?: return
        updateState {
            copy(
                formState = TextActionFormState(
                    editingIndex = index,
                    previousId = item.id,
                    id = item.id,
                    name = item.name,
                    promptInput = item.promptTemplate,
                ).withCurrentSnapshotAsInitial(),
                inlineError = null,
            )
        }
    }

    /**
     * 静默开关前置：悬浮球或常驻通知必须任一启用（后台回合需要保活载体）。
     * 不满足时回滚开关并发一条 toast。读 flow 的当前值即可——两个开关都是
     * ReactiveAppStateField，设置页改动即时可见。
     */
    private suspend fun toggleSilent() {
        val next = !currentState.silentEnabled
        if (next) {
            // 挂起 getter 读真值（不依赖 MainActivity 是否已 hydrateSettings）。
            val carrierPresent =
                XRepo.floatingBallEnabled() || XRepo.residentNotificationEnabled()
            if (!carrierPresent) {
                sendEffect(TextActionsEffect.SilentNeedsCarrier)
                return
            }
        }
        updateState { copy(silentEnabled = next) }
        try {
            XRepo.setTextActionSilent(next)
            Logger.i(LOG_TAG, "toggleSilent value=$next")
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            Logger.w(LOG_TAG, "toggleSilent failed reason=${throwable.message}")
            updateState { copy(silentEnabled = !next) }
        }
    }

    private suspend fun toggleItemEnabled(index: Int, enabled: Boolean) {
        val currentItem = currentState.items.getOrNull(index) ?: return
        val previousItems = currentState.items
        val updatedItems = currentState.items.toMutableList().apply {
            this[index] = currentItem.copy(enabled = enabled)
        }
        updateState {
            copy(
                items = updatedItems,
                isSaving = true,
                inlineError = null,
            )
        }
        try {
            XRepo.textActions.setEnabled(currentItem.id, enabled)
            updateState { copy(isSaving = false) }
            notifySettingsChanged()
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            Logger.w(LOG_TAG, "toggleItemEnabled failed reason=${throwable.message}")
            updateState {
                copy(
                    items = previousItems,
                    isSaving = false,
                    inlineError = TextActionsInlineError.SaveFailed(
                        message = throwable.message,
                        fallbackResId = R.string.error_text_actions_save_failed,
                    ),
                )
            }
        }
    }

    private suspend fun save() {
        val formState = currentState.formState
        val normalizedName = formState.name.trim()
        val normalizedPrompt = formState.promptInput.trim()
        if (normalizedName.isBlank() || normalizedPrompt.isBlank()) {
            val nameErrorResId = if (normalizedName.isBlank()) {
                R.string.text_actions_error_name_required
            } else {
                null
            }
            val promptErrorResId = if (normalizedPrompt.isBlank()) {
                R.string.text_actions_error_prompt_required
            } else {
                null
            }
            updateState {
                copy(
                    formState = formState.copy(
                        name = normalizedName,
                        promptInput = normalizedPrompt,
                        nameErrorResId = nameErrorResId,
                        promptErrorResId = promptErrorResId,
                    ),
                    isSaving = false,
                    inlineError = null,
                )
            }
            sendEffect(
                if (nameErrorResId != null) TextActionsEffect.FocusName else TextActionsEffect.FocusPrompt
            )
            return
        }

        val normalizedFormState = formState.copy(
            name = normalizedName,
            promptInput = normalizedPrompt,
        )
        updateState {
            copy(
                isSaving = true,
                formState = normalizedFormState.copy(
                    nameErrorResId = null,
                    promptErrorResId = null,
                ),
                inlineError = null,
            )
        }
        try {
            val nextAction = TextAction(
                id = normalizedFormState.id ?: newActionId(),
                name = normalizedFormState.name,
                promptTemplate = normalizedFormState.promptInput,
                enabled = true,
            )
            XRepo.textActions.replace(
                previousId = normalizedFormState.previousId,
                action = nextAction,
            )
            Logger.i(LOG_TAG, "save succeeded actionId=${nextAction.id}")
            val nextItem = nextAction.toItem()
            val updatedItems = currentState.items.toMutableList().also { mutableItems ->
                val editingIndex = normalizedFormState.editingIndex
                if (editingIndex == null || editingIndex !in mutableItems.indices) {
                    mutableItems += nextItem
                } else {
                    mutableItems[editingIndex] = nextItem
                }
            }
            updateState {
                copy(
                    items = updatedItems,
                    formState = normalizedFormState.copy(
                        editingIndex = updatedItems.indexOf(nextItem),
                        previousId = nextAction.id,
                        id = nextAction.id,
                    ).withCurrentSnapshotAsInitial(),
                    isSaving = false,
                    inlineError = null,
                )
            }
            notifySettingsChanged()
            sendEffect(TextActionsEffect.ExitDetail)
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            Logger.w(LOG_TAG, "save failed reason=${throwable.message}")
            updateState {
                copy(
                    isSaving = false,
                    inlineError = TextActionsInlineError.SaveFailed(
                        message = throwable.message,
                        fallbackResId = R.string.error_text_actions_save_failed,
                    ),
                )
            }
        }
    }

    private fun requestDelete() {
        val editingIndex = currentState.formState.editingIndex ?: return
        val value = currentState.items.getOrNull(editingIndex)?.name ?: return
        updateState {
            copy(
                deleteConfirmation = TextActionDeleteConfirmationState(value = value),
                inlineError = null,
            )
        }
    }

    private suspend fun confirmDelete() {
        val confirmation = currentState.deleteConfirmation ?: return
        updateState { copy(deleteConfirmation = null) }
        deleteCurrent()
    }

    private suspend fun deleteCurrent() {
        val currentId = currentState.formState.id ?: return
        val editingIndex = currentState.formState.editingIndex
        updateState { copy(isSaving = true) }
        try {
            XRepo.textActions.delete(currentId)
            Logger.i(LOG_TAG, "deleteCurrent succeeded actionId=$currentId")
            val updatedItems = if (editingIndex != null) {
                currentState.items.filterIndexed { index, _ -> index != editingIndex }
            } else {
                currentState.items.filterNot { it.id == currentId }
            }
            updateState {
                copy(
                    items = updatedItems,
                    formState = TextActionFormState(),
                    isSaving = false,
                    inlineError = null,
                )
            }
            notifySettingsChanged()
            sendEffect(TextActionsEffect.ExitDetail)
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            Logger.w(LOG_TAG, "deleteCurrent failed reason=${throwable.message}")
            updateState {
                copy(
                    isSaving = false,
                    inlineError = TextActionsInlineError.DeleteFailed(
                        message = throwable.message,
                        fallbackResId = R.string.error_text_actions_delete_failed,
                    ),
                )
            }
        }
    }

    private fun notifySettingsChanged() {
        settingsChanges.tryEmit(Unit)
    }

    private companion object {
        private const val LOG_TAG = "niki914_zafiro_TextActionsViewModel"
        val settingsChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    }
}

private fun TextAction.toItem(): TextActionItem {
    return TextActionItem(
        id = id,
        name = name,
        promptTemplate = promptTemplate,
        enabled = enabled,
    )
}

private fun TextActionFormState.toSnapshot(): TextActionFormSnapshot {
    return TextActionFormSnapshot(
        name = name.trim(),
        prompt = promptInput.trim(),
    )
}

private fun TextActionFormState.withCurrentSnapshotAsInitial(): TextActionFormState {
    return copy(initialSnapshot = toSnapshot())
}

private fun newActionId(): String {
    return "custom-${UUID.randomUUID()}"
}
