package com.niki914.nexus.agentic.app.ui.nexus.model

import com.niki914.nexus.cb.ComposeMVIViewModel

data class ExecutionRuleItem(
    val name: String,
    val enabled: Boolean,
)

data class ExecutionRulesSettingsUiState(
    val items: List<ExecutionRuleItem> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
)

sealed interface ExecutionRulesSettingsIntent {
    data object Load : ExecutionRulesSettingsIntent
    data class ItemEnabledChanged(val index: Int, val value: Boolean) : ExecutionRulesSettingsIntent
}

class ExecutionRulesSettingsViewModel :
    ComposeMVIViewModel<ExecutionRulesSettingsIntent, ExecutionRulesSettingsUiState, Nothing>() {

    override fun initUiState(): ExecutionRulesSettingsUiState = ExecutionRulesSettingsUiState()

    override suspend fun handleIntent(intent: ExecutionRulesSettingsIntent) {
        when (intent) {
            ExecutionRulesSettingsIntent.Load -> load()
            is ExecutionRulesSettingsIntent.ItemEnabledChanged -> toggleItemEnabled(
                index = intent.index,
                enabled = intent.value,
            )
        }
    }

    private fun load() {
        updateState {
            copy(isLoading = false)
        }
    }

    private fun toggleItemEnabled(index: Int, enabled: Boolean) {
        val currentItem = currentState.items.getOrNull(index) ?: return
        val updatedItems = currentState.items.toMutableList().apply {
            this[index] = currentItem.copy(enabled = enabled)
        }
        updateState {
            copy(items = updatedItems)
        }
    }
}
