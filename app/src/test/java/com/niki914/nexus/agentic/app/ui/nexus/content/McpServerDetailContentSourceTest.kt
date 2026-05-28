package com.niki914.nexus.agentic.app.ui.nexus.content

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class McpServerDetailContentSourceTest {
    private val sourceFile = File(
        "src/main/java/com/niki914/nexus/agentic/app/ui/nexus/content/mcp/McpServerDetailContent.kt",
    )

    @Test
    fun mcp_detail_content_matches_t03_structure_contract() {
        val source = sourceFile.readText()

        assertTrue(
            source.contains("package com.niki914.nexus.agentic.app.ui.nexus.content.mcp"),
        )
        assertTrue(source.contains("page: McpServerDetailPage"))
        assertTrue(source.contains("onBack: () -> Unit"))
        assertTrue(
            source.contains(
                "pageViewModel<McpSettingsViewModel>(factory = McpSettingsViewModelFactory)",
            ),
        )
        assertTrue(source.contains("LaunchedEffect(page.routeKey)"))
        assertTrue(source.contains("McpSettingsIntent.StartCreate"))
        assertTrue(source.contains("McpSettingsIntent.StartEdit(page.serverIndex)"))
        assertTrue(source.contains("SettingsDetailFormScaffold("))
        assertTrue(source.contains("McpIdentitySettingsBlock("))
        assertTrue(source.contains("McpConnectionSettingsBlock("))
        assertTrue(source.contains("SettingExpandableTextItem("))
        assertTrue(source.contains("SettingToggleItem("))
        assertTrue(source.contains("SettingsItemDivider("))
        assertTrue(source.contains("McpSettingsIntent.Save"))
        assertTrue(source.contains("McpSettingsEffect.ExitDetail"))
        assertTrue(source.contains("onBack()"))
        assertTrue(source.contains("placeholder = stringResource(R.string.mcp_field_name_hint)"))
        assertTrue(source.contains("placeholder = stringResource(R.string.mcp_field_url_hint)"))
        assertTrue(source.contains("placeholder = stringResource(R.string.mcp_field_headers_hint)"))
        assertTrue(source.contains("description = mcpFieldErrorText("))
        assertTrue(source.contains("requestedFocusField"))
        assertTrue(source.contains("LaunchedEffect(requestedFocusField)"))
        assertTrue(source.contains("McpSettingsEffect.FocusName"))
        assertTrue(source.contains("McpSettingsEffect.FocusUrl"))
        assertTrue(source.contains("McpSettingsEffect.FocusHeaders"))
        assertTrue(source.contains("clearActiveField()"))
        assertTrue(source.contains("viewModel.sendIntent(McpSettingsIntent.EnabledChanged(value))"))
        assertTrue(source.contains("@Preview"))
        assertTrue(source.contains("private fun McpServerDetailContentPreview()"))
        assertFalse(source.contains("McpItemDivider("))
        assertFalse(source.contains("McpHintText("))
        assertFalse(source.contains("description = null"))
        assertFalse(
            source.contains("description = stringResource(R.string.mcp_field_enabled_description)"),
        )
    }
}
