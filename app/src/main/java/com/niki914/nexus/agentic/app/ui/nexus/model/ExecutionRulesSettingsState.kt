package com.niki914.nexus.agentic.app.ui.nexus.model

import com.niki914.nexus.cb.ComposeMVIViewModel

data class ExecutionRuleDraft(
    val id: String,
    val note: String,
    val mode: ExecutionRuleMode,
    val matcherType: ExecutionRuleMatcherType,
    val keywords: List<String>,
    val regex: String,
)

enum class ExecutionRuleMode {
    Blacklist,
    Whitelist,
}

enum class ExecutionRuleMatcherType {
    Keywords,
    Regex,
}

data class ExecutionRulesSettingsUiState(
    val rules: List<ExecutionRuleDraft> = emptyList(),
    val editingRule: ExecutionRuleDraft? = null,
)

sealed interface ExecutionRulesSettingsIntent {
    data object Load : ExecutionRulesSettingsIntent
    data object StartCreate : ExecutionRulesSettingsIntent
    data class StartEdit(val id: String) : ExecutionRulesSettingsIntent
    data class ModeChanged(val value: ExecutionRuleMode) : ExecutionRulesSettingsIntent
    data class NoteChanged(val value: String) : ExecutionRulesSettingsIntent
    data class MatcherTypeChanged(val value: ExecutionRuleMatcherType) : ExecutionRulesSettingsIntent
    data class KeywordsChanged(val values: List<String>) : ExecutionRulesSettingsIntent
    data class RegexChanged(val value: String) : ExecutionRulesSettingsIntent
    data object SaveEditingRule : ExecutionRulesSettingsIntent
    data object DismissEditingRule : ExecutionRulesSettingsIntent
}

class ExecutionRulesSettingsViewModel :
    ComposeMVIViewModel<
            ExecutionRulesSettingsIntent,
            ExecutionRulesSettingsUiState,
            Nothing,
            >() {

    override fun initUiState(): ExecutionRulesSettingsUiState = ExecutionRulesSettingsUiState()

    override suspend fun handleIntent(intent: ExecutionRulesSettingsIntent) {
        when (intent) {
            ExecutionRulesSettingsIntent.Load -> loadSampleRules()
            ExecutionRulesSettingsIntent.StartCreate -> startCreate()
            is ExecutionRulesSettingsIntent.StartEdit -> startEdit(intent.id)
            is ExecutionRulesSettingsIntent.ModeChanged -> updateEditingRule {
                copy(mode = intent.value)
            }

            is ExecutionRulesSettingsIntent.NoteChanged -> updateEditingRule {
                copy(note = intent.value)
            }

            is ExecutionRulesSettingsIntent.MatcherTypeChanged -> updateEditingRule {
                copy(matcherType = intent.value)
            }

            is ExecutionRulesSettingsIntent.KeywordsChanged -> updateEditingRule {
                copy(keywords = intent.values.map(String::trim).filter(String::isNotBlank))
            }

            is ExecutionRulesSettingsIntent.RegexChanged -> updateEditingRule {
                copy(regex = intent.value)
            }

            ExecutionRulesSettingsIntent.SaveEditingRule -> saveEditingRule()
            ExecutionRulesSettingsIntent.DismissEditingRule -> updateState {
                copy(editingRule = null)
            }
        }
    }

    private fun loadSampleRules() {
        if (currentState.rules.isNotEmpty()) {
            return
        }
        updateState {
            copy(
                rules = listOf(
                    ExecutionRuleDraft(
                        id = "sample-blacklist-delete",
                        note = "禁止高风险删除命令",
                        mode = ExecutionRuleMode.Blacklist,
                        matcherType = ExecutionRuleMatcherType.Keywords,
                        keywords = listOf("rm -rf", "format", "wipe"),
                        regex = "",
                    ),
                    ExecutionRuleDraft(
                        id = "sample-whitelist-readonly",
                        note = "允许只读诊断命令",
                        mode = ExecutionRuleMode.Whitelist,
                        matcherType = ExecutionRuleMatcherType.Regex,
                        keywords = emptyList(),
                        regex = "^(ls|pwd|cat|logcat)\\b.*",
                    ),
                ),
            )
        }
    }

    private fun startCreate() {
        updateState {
            copy(
                editingRule = ExecutionRuleDraft(
                    id = "draft-${rules.size + 1}",
                    note = "",
                    mode = ExecutionRuleMode.Blacklist,
                    matcherType = ExecutionRuleMatcherType.Keywords,
                    keywords = emptyList(),
                    regex = "",
                ),
            )
        }
    }

    private fun startEdit(id: String) {
        val rule = currentState.rules.firstOrNull { it.id == id } ?: return
        updateState {
            copy(editingRule = rule)
        }
    }

    private fun saveEditingRule() {
        val editingRule = currentState.editingRule ?: return
        val normalizedRule = editingRule.copy(
            note = editingRule.note.trim().ifBlank { "未命名执行规则" },
            keywords = editingRule.keywords.map(String::trim).filter(String::isNotBlank),
            regex = editingRule.regex.trim(),
        )
        updateState {
            val updatedRules = rules.toMutableList()
            val index = updatedRules.indexOfFirst { it.id == normalizedRule.id }
            if (index >= 0) {
                updatedRules[index] = normalizedRule
            } else {
                updatedRules += normalizedRule
            }
            copy(
                rules = updatedRules,
                editingRule = null,
            )
        }
    }

    private fun updateEditingRule(transform: ExecutionRuleDraft.() -> ExecutionRuleDraft) {
        val editingRule = currentState.editingRule ?: return
        updateState {
            copy(editingRule = editingRule.transform())
        }
    }
}
