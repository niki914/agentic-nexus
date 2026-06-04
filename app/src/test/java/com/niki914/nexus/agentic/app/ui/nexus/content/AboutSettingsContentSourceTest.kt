package com.niki914.nexus.agentic.app.ui.nexus.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AboutSettingsContentSourceTest {
    @Test
    fun aboutSettingsContent_containsSingleCardFiveItemsAndVersionState() {
        val source = readAboutSettingsContentSource()

        assertTrue(source.contains("SettingsListPageContent("))
        assertTrue(source.contains("SettingsGroupCard"))
        assertEquals(5, source.countOccurrences("SettingsListItem("))
        assertTrue(source.contains("BuildConfig.VERSION_NAME"))
        assertTrue(source.contains("R.string.ui_settings_about_author_homepage"))
        assertTrue(source.contains("R.string.ui_settings_about_github"))
        assertTrue(source.contains("R.string.ui_settings_about_afdian"))
        assertTrue(source.contains("R.string.ui_settings_about_telegram"))
        assertTrue(source.contains("R.string.ui_settings_about_version"))
    }

    @Test
    fun aboutSettingsContent_declaresLightAndDarkPreviews() {
        val source = readAboutSettingsContentSource()

        assertTrue(source.contains("@Preview(name = \"About Settings Light\""))
        assertTrue(source.contains("name = \"About Settings Dark\""))
        assertTrue(source.contains("ProvideLiquidScreenContentForPreview(topPadding = 0.dp)"))
        assertTrue(source.contains("BaseTheme(darkTheme = false, dynamicColor = false)"))
        assertTrue(source.contains("BaseTheme(darkTheme = true, dynamicColor = false)"))
    }

    @Test
    fun settingsDetailPage_routesAboutGroupToAboutSettingsContent() {
        val source = File(
            "src/main/java/com/niki914/nexus/agentic/app/ui/nexus/content/SettingsDetailPageContent.kt"
        ).readText()

        assertTrue(source.contains("group == NexusSettingsGroup.About"))
        assertTrue(source.contains("AboutSettingsContent()"))
    }

    private fun readAboutSettingsContentSource(): String {
        return File(
            "src/main/java/com/niki914/nexus/agentic/app/ui/nexus/content/AboutSettingsContent.kt"
        ).readText()
    }

    private fun String.countOccurrences(needle: String): Int {
        return split(needle).size - 1
    }
}
