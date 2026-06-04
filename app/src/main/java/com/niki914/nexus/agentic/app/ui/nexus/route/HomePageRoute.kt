package com.niki914.nexus.agentic.app.ui.nexus.route

import androidx.compose.runtime.Composable
import com.niki914.nexus.agentic.app.ui.nexus.content.HomePageContent
import com.niki914.nexus.agentic.app.ui.nexus.nav.NexusPage
import com.niki914.nexus.agentic.app.ui.nexus.nav.SettingsHomePage

@Composable
internal fun HomePageRoute(
    onPush: (NexusPage) -> Unit,
) {
    HomePageContent(
        onOpenSettings = {
            onPush(SettingsHomePage)
        },
    )
}
