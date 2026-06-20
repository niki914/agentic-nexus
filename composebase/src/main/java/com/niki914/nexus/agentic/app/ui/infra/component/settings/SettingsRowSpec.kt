package com.niki914.nexus.agentic.app.ui.infra.component.settings

sealed interface SettingsRowSpec {
    val id: String?
    val title: String
    val enabled: Boolean

    data class Navigation(
        override val id: String,
        override val title: String,
        val summary: String? = null,
        val currentState: String? = null,
        override val enabled: Boolean = true,
    ) : SettingsRowSpec

    data class Toggle(
        override val id: String,
        override val title: String,
        val summary: String? = null,
        val checked: Boolean,
        override val enabled: Boolean = true,
    ) : SettingsRowSpec

    data class ToggleNavigation(
        override val id: String,
        override val title: String,
        val checked: Boolean,
        override val enabled: Boolean = true,
    ) : SettingsRowSpec

    data class Value(
        override val title: String,
        val summary: String? = null,
        val currentState: String,
        override val enabled: Boolean = true,
    ) : SettingsRowSpec {
        override val id: String? = null
    }

    data class Action(
        override val id: String,
        override val title: String,
        val summary: String? = null,
        override val enabled: Boolean = true,
    ) : SettingsRowSpec

    data class Destructive(
        override val id: String,
        override val title: String,
        val summary: String? = null,
        override val enabled: Boolean = true,
    ) : SettingsRowSpec

    data class Message(
        override val title: String,
    ) : SettingsRowSpec {
        override val id: String? = null
        override val enabled: Boolean = true
    }
}
