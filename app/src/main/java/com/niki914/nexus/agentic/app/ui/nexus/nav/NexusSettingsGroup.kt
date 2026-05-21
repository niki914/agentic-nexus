package com.niki914.nexus.agentic.app.ui.nexus.nav

import androidx.annotation.StringRes
import com.niki914.nexus.agentic.app.R

enum class NexusSettingsGroup(
    @StringRes val titleRes: Int,
    @StringRes val summaryRes: Int,
    val routeSuffix: String,
) {
    ProviderModel(
        titleRes = R.string.nexus_settings_provider_model,
        summaryRes = R.string.nexus_settings_provider_model_summary,
        routeSuffix = "provider-model",
    ),
    Network(
        titleRes = R.string.nexus_settings_network,
        summaryRes = R.string.nexus_settings_network_summary,
        routeSuffix = "network",
    ),
    Memory(
        titleRes = R.string.nexus_settings_memory,
        summaryRes = R.string.nexus_settings_memory_summary,
        routeSuffix = "memory",
    ),
    BuiltinTools(
        titleRes = R.string.nexus_settings_builtin_tools,
        summaryRes = R.string.nexus_settings_builtin_tools_summary,
        routeSuffix = "builtin-tools",
    ),
    ShellRules(
        titleRes = R.string.nexus_settings_shell_rules,
        summaryRes = R.string.nexus_settings_shell_rules_summary,
        routeSuffix = "shell-rules",
    ),
    Mcp(
        titleRes = R.string.nexus_settings_mcp,
        summaryRes = R.string.nexus_settings_mcp_summary,
        routeSuffix = "mcp",
    ),
    CustomTools(
        titleRes = R.string.nexus_settings_custom_tools,
        summaryRes = R.string.nexus_settings_custom_tools_summary,
        routeSuffix = "custom-tools",
    ),
    About(
        titleRes = R.string.nexus_settings_about,
        summaryRes = R.string.nexus_settings_about_summary,
        routeSuffix = "about",
    ),
}
