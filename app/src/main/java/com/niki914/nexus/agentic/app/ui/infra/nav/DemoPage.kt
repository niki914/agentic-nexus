package com.niki914.nexus.agentic.app.ui.infra.nav

sealed interface DemoPage : Page {
    val title: String
    val showLeftButton: Boolean
    val showRightButton: Boolean
}

data object HomePage : DemoPage {
    override val routeKey: String = "home"
    override val title: String = "Homepage"
    override val showLeftButton: Boolean = false
    override val showRightButton: Boolean = true
}

data object MorePage : DemoPage {
    override val routeKey: String = "more"
    override val title: String = "More"
    override val showLeftButton: Boolean = true
    override val showRightButton: Boolean = false
}

data class SettingsGroupPage(
    val groupId: String,
) : DemoPage {
    override val routeKey: String = "settings-group:$groupId"
    override val title: String = "Settings Group ${groupId.toDisplaySuffix()}"
    override val showLeftButton: Boolean = true
    override val showRightButton: Boolean = false
}

data class SubSettingPage(
    val settingId: String,
) : DemoPage {
    override val routeKey: String = "sub-setting:$settingId"
    override val title: String = "Sub Setting ${settingId.toDisplaySuffix()}"
    override val showLeftButton: Boolean = true
    override val showRightButton: Boolean = false
}

private fun String.toDisplaySuffix(): String {
    val digits = takeLastWhile { it.isDigit() }
    return digits.ifEmpty { this }
}
