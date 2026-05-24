package com.niki914.nexus.agentic.app.ui.nexus.nav

import com.niki914.nexus.agentic.app.R
import com.niki914.nexus.agentic.app.ui.infra.nav.Page

sealed interface NexusPage : Page {
    val titleRes: Int?
    val showLeftButton: Boolean
    val showRightButton: Boolean
    val showBlurLayer: Boolean
        get() = true
}

data object StartupPage : NexusPage {
    override val routeKey: String = "startup"
    override val titleRes: Int? = null
    override val showLeftButton: Boolean = false
    override val showRightButton: Boolean = false
    override val showBlurLayer: Boolean = false
}

data object ProviderPickPage : NexusPage {
    override val routeKey: String = "provider-pick"
    override val titleRes: Int = R.string.nexus_provider_pick_title
    override val showLeftButton: Boolean = true
    override val showRightButton: Boolean = false
}

data class ConfigurePage(
    val providerId: String? = null,
) : NexusPage {
    override val routeKey: String = if (providerId == null) "configure" else "configure:$providerId"
    override val titleRes: Int = R.string.nexus_configure_title
    override val showLeftButton: Boolean = true
    override val showRightButton: Boolean = false
}

data object DonePage : NexusPage {
    override val routeKey: String = "done"
    override val titleRes: Int? = null
    override val showLeftButton: Boolean = true
    override val showRightButton: Boolean = false
}

data object HomePage : NexusPage {
    override val routeKey: String = "home"
    override val titleRes: Int = R.string.nexus_home_title
    override val showLeftButton: Boolean = false
    override val showRightButton: Boolean = true
}

data object SettingsHomePage : NexusPage {
    override val routeKey: String = "settings-home"
    override val titleRes: Int = R.string.nexus_settings_title
    override val showLeftButton: Boolean = true
    override val showRightButton: Boolean = false
}

data class SettingsDetailPage(
    val group: NexusSettingsGroup,
) : NexusPage {
    override val routeKey: String = "settings-detail:${group.routeSuffix}"
    override val titleRes: Int = group.titleRes
    override val showLeftButton: Boolean = true
    override val showRightButton: Boolean = false
}
