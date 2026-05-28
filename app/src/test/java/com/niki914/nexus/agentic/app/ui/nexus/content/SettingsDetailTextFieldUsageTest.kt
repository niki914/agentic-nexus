package com.niki914.nexus.agentic.app.ui.nexus.content

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsDetailTextFieldUsageTest {

    private val mainSourceRoot: Path = Paths.get("src/main/java")
    private val mcpSettingsPath: Path =
        mainSourceRoot.resolve(
            "com/niki914/nexus/agentic/app/ui/nexus/content/mcp/McpSettingsContent.kt"
        )
    private val customToolsSettingsPath: Path =
        mainSourceRoot.resolve(
            "com/niki914/nexus/agentic/app/ui/nexus/content/CustomToolsSettingsContent.kt"
        )
    private val liquidTextFieldPath: Path =
        mainSourceRoot.resolve(
            "com/niki914/nexus/agentic/app/ui/infra/component/LiquidTextField.kt"
        )

    @Test
    fun main_sources_no_longer_reference_styled_text_field() {
        val usages = Files.walk(mainSourceRoot).use { paths ->
            paths
                .filter { it.isRegularFile() && it.extension == "kt" }
                .filter { path ->
                    path.readText().contains("StyledTextField(")
                }
                .map { mainSourceRoot.relativize(it).toString() }
                .toList()
        }

        assertTrue(
            "Expected StyledTextField usages to be fully removed, but found: $usages",
            usages.isEmpty(),
        )
    }

    @Test
    fun settings_detail_editors_use_liquid_text_field() {
        val mcpSettingsContent = mcpSettingsPath.readText()
        val customToolsSettingsContent = customToolsSettingsPath.readText()

        assertTrue(mcpSettingsContent.contains("LiquidTextField("))
        assertTrue(customToolsSettingsContent.contains("LiquidTextField("))
        assertFalse(mcpSettingsContent.contains("StyledTextField("))
        assertFalse(customToolsSettingsContent.contains("StyledTextField("))
    }

    @Test
    fun liquid_text_field_exposes_min_lines_for_settings_editors() {
        val liquidTextField = liquidTextFieldPath.readText()

        assertTrue(liquidTextField.contains("minLines: Int = 1"))
        assertTrue(liquidTextField.contains("minLines = minLines"))
    }

    private fun Path.readText(): String = String(Files.readAllBytes(this))
}
