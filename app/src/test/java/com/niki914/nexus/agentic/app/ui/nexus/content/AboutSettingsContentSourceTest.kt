package com.niki914.nexus.agentic.app.ui.nexus.content

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AboutSettingsContentSourceTest {
    @Test
    fun aboutSettingsContent_containsAboutLinksFeedbackAndVersionState() {
        val source = readAboutSettingsContentSource()

        assertTrue(source.contains("SettingsListPageContent("))
        assertTrue(source.contains("SettingsGroupCard"))
        assertTrue(source.contains("uiState.items.forEach"))
        assertTrue(source.contains("BuildConfig.VERSION_NAME"))
        assertTrue(source.contains("R.string.ui_settings_about_author_homepage"))
        assertTrue(source.contains("R.string.ui_settings_about_github"))
        assertTrue(source.contains("R.string.ui_settings_about_afdian"))
        assertTrue(source.contains("R.string.ui_settings_about_telegram"))
        assertTrue(source.contains("R.string.ui_settings_about_feedback_feature"))
        assertTrue(source.contains("R.string.ui_settings_about_feedback_bug"))
        assertTrue(source.contains("R.string.ui_settings_about_version"))
    }

    @Test
    fun aboutSettingsContent_usesViewModelEffectsToOpenUris() {
        val source = readAboutSettingsContentSource()

        assertTrue(source.contains("pageViewModel<AboutSettingsViewModel>()"))
        assertTrue(source.contains("AboutSettingsIntent.OpenItem"))
        assertTrue(source.contains("AboutSettingsEffect.OpenUri"))
        assertTrue(source.contains("openUri(effect.uri)"))
    }

    @Test
    fun aboutSettingsViewModel_keepsIssueTemplatesReadableAndEncodesWhenOpening() {
        val source = File(
            "src/main/java/com/niki914/nexus/agentic/app/ui/nexus/model/AboutSettingsState.kt"
        ).readText()

        assertTrue(source.contains("FEATURE_FEEDBACK_TITLE"))
        assertTrue(source.contains("FEATURE_FEEDBACK_BODY"))
        assertTrue(source.contains("BUG_FEEDBACK_TITLE"))
        assertTrue(source.contains("BUG_FEEDBACK_BODY"))
        assertTrue(source.contains("Uri.encode(title)"))
        assertTrue(source.contains("Uri.encode(body)"))
        assertTrue(source.contains("\"[Feature] \""))
        assertTrue(source.contains("\"[Bug] \""))
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

}
