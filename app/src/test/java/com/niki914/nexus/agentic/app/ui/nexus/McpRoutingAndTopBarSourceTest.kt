package com.niki914.nexus.agentic.app.ui.nexus

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class McpRoutingAndTopBarSourceTest {
    private val legacyMcpDetailFile = File(
        "src/main/java/com/niki914/nexus/agentic/app/ui/nexus/content/McpServerDetailContent.kt",
    )
    private val mcpSettingsContentFile = File(
        "src/main/java/com/niki914/nexus/agentic/app/ui/nexus/content/mcp/McpSettingsContent.kt",
    )
    private val mcpServerDetailContentFile = File(
        "src/main/java/com/niki914/nexus/agentic/app/ui/nexus/content/mcp/McpServerDetailContent.kt",
    )
    private val settingsDetailPageContentFile = File(
        "src/main/java/com/niki914/nexus/agentic/app/ui/nexus/content/SettingsDetailPageContent.kt",
    )
    private val nexusPagesFile = File(
        "src/main/java/com/niki914/nexus/agentic/app/ui/nexus/NexusPages.kt",
    )
    private val nexusAppFile = File(
        "src/main/java/com/niki914/nexus/agentic/app/ui/nexus/NexusApp.kt",
    )

    @Test
    fun mcp_settings_content_is_moved_into_shared_mcp_package() {
        val listSource = mcpSettingsContentFile.readText()
        val detailSource = mcpServerDetailContentFile.readText()

        assertFalse(legacyMcpDetailFile.exists())
        assertTrue(
            listSource.contains("package com.niki914.nexus.agentic.app.ui.nexus.content.mcp"),
        )
        assertTrue(
            listSource.contains(
                "pageViewModel<McpSettingsViewModel>()",
            ),
        )
        assertTrue(listSource.contains("LaunchedEffect(Unit)"))
        assertTrue(listSource.contains("McpSettingsIntent.Load"))
        assertFalse(detailSource.contains("McpSettingsViewModelFactory"))
    }

    @Test
    fun mcp_page_dispatch_uses_content_mcp_package_and_passes_detail_arguments() {
        val settingsDetailSource = settingsDetailPageContentFile.readText()
        val nexusPagesSource = nexusPagesFile.readText()

        assertTrue(
            settingsDetailSource.contains(
                "import com.niki914.nexus.agentic.app.ui.nexus.content.mcp.McpSettingsContent",
            ),
        )
        assertTrue(
            settingsDetailSource.contains(
                "McpSettingsContent(",
            ),
        )
        assertTrue(
            nexusPagesSource.contains(
                "McpServerDetailContent(",
            ),
        )
        assertTrue(nexusPagesSource.contains("page = page"))
        assertTrue(nexusPagesSource.contains("onBack = onPop"))
    }

    @Test
    fun nexus_app_binds_delete_action_to_current_mcp_detail_entry() {
        val source = nexusAppFile.readText()

        assertFalse(source.contains("currentPage is McpServerDetailPage"))
        assertFalse(source.contains("McpSettingsViewModelFactory"))
        assertFalse(source.contains("McpSettingsIntent.DeleteCurrent"))
        assertFalse(source.contains("ViewModelProvider(currentEntry"))
    }
}
