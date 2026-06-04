package com.niki914.nexus.agentic.app.ui.nexus

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class NexusPagesRouteSourceTest {
    @Test
    fun nexusPagesContent_onlyKeepsRouteDispatchResponsibilities() {
        val source = File(
            "src/main/java/com/niki914/nexus/agentic/app/ui/nexus/NexusPages.kt"
        ).readText()

        assertFalse(source.contains("pageViewModel<"))
        assertFalse(source.contains("@Preview"))
        assertFalse(source.contains("StartupWebSettingsDialog"))

        assertTrue(source.contains("StartupPageRoute("))
        assertTrue(source.contains("ProviderPickPageRoute("))
        assertTrue(source.contains("ConfigurePageRoute("))
        assertTrue(source.contains("DonePageRoute("))
        assertTrue(source.contains("HomePageRoute("))
        assertTrue(source.contains("SettingsHomePageRoute("))
        assertTrue(source.contains("SettingsDetailPageRoute("))
        assertTrue(source.contains("McpServerDetailRoute("))
        assertTrue(source.contains("ExecutionRuleDetailRoute("))
        assertTrue(source.contains("CustomToolDetailRoute("))
    }
}
