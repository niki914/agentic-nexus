package com.niki914.nexus.agentic.app.ui.infra.component

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SettingsListItemSourceTest {
    @Test
    fun settingsListItem_usesLeadingSpecificPaddingAndTighterSpacing() {
        val source = readSettingsListItemSource()

        assertTrue(source.contains("contentPadding = if (leadingContent != null)"))
        assertTrue(source.contains("start = 20.dp"))
        assertTrue(source.contains("Arrangement.spacedBy(if (leadingContent != null) 6.dp else 8.dp)"))
    }

    @Test
    fun settingsListItem_onlyRendersSummaryWhenNonNull() {
        val source = readSettingsListItemSource()

        assertTrue(source.contains("if (summary != null)"))
    }

    private fun readSettingsListItemSource(): String {
        return File(
            "src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingsListItem.kt"
        ).readText()
    }
}
