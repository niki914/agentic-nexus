package com.niki914.nexus.agentic.app.ui.nexus.content

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsDetailScaffoldSourceTest {
    private val scaffoldFile = File(
        "../composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingsDetailFormScaffold.kt",
    )
    private val defaultsFile = File(
        "../composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingsDetailPageDefaults.kt",
    )
    private val configureContentFile = File(
        "src/main/java/com/niki914/nexus/agentic/app/ui/nexus/content/ConfigurePageContent.kt",
    )
    private val providerBlockFile = File(
        "src/main/java/com/niki914/nexus/agentic/app/ui/nexus/content/ProviderAccessSettingsBlock.kt",
    )
    private val mcpContentFile = File(
        "src/main/java/com/niki914/nexus/agentic/app/ui/nexus/content/mcp/McpServerDetailContent.kt",
    )

    @Test
    fun settings_detail_scaffold_migration_contract() {
        assertExists(scaffoldFile, "缺少公共详情页 scaffold")
        assertExists(defaultsFile, "缺少公共详情页 defaults/divider helper")

        val scaffoldSource = scaffoldFile.readText()
        val defaultsSource = defaultsFile.readText()
        val configureSource = configureContentFile.readText()
        val providerBlockSource = providerBlockFile.readText()
        val mcpSource = mcpContentFile.readText()

        assertSourceContains(
            scaffoldSource,
            "fun SettingsDetailFormScaffold(",
            "公共 scaffold 未声明 SettingsDetailFormScaffold",
        )
        assertSourceContains(
            defaultsSource,
            "fun SettingsItemDivider(",
            "公共 defaults 未声明 SettingsItemDivider",
        )
        assertSourceContains(
            configureSource,
            "SettingsDetailFormScaffold(",
            "ConfigurePageContent 未迁移到 SettingsDetailFormScaffold",
        )
        assertSourceContains(
            mcpSource,
            "SettingsDetailFormScaffold(",
            "McpServerDetailContent 未迁移到 SettingsDetailFormScaffold",
        )
        assertSourceContains(
            providerBlockSource,
            "SettingsItemDivider(",
            "ProviderAccessSettingsBlock 未使用公共 SettingsItemDivider",
        )
        assertSourceContains(
            mcpSource,
            "SettingsItemDivider(",
            "McpServerDetailContent 的 MCP block 未使用公共 SettingsItemDivider",
        )
    }

    private fun assertExists(file: File, message: String) {
        assertTrue("$message: ${file.path}", file.exists())
    }

    private fun assertSourceContains(source: String, expected: String, message: String) {
        assertTrue("$message，缺失片段: $expected", source.contains(expected))
    }
}
