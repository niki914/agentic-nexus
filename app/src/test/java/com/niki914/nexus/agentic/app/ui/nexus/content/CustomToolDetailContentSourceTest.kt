package com.niki914.nexus.agentic.app.ui.nexus.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CustomToolDetailContentSourceTest {
    @Test
    fun customToolDetailContent_usesDetailScaffoldAndChromeContract() {
        val source = readCustomToolDetailContentSource()

        assertTrue(source.contains("SettingsDetailFormScaffold("))
        assertTrue(source.contains("RegisterPageChrome(pageChromeContribution)"))
        assertTrue(source.contains("PageChromeContribution.Empty"))
        assertTrue(source.contains("PageChromeContribution("))
        assertTrue(source.contains("Icons.Default.Delete"))
        assertTrue(source.contains("CustomToolSettingsIntent.DeleteCurrent"))
        assertTrue(source.contains("onBackgroundTap = ::clearActiveField"))
        assertTrue(source.contains("requestedFocusField"))
    }

    @Test
    fun customToolDetailContent_containsExpectedFieldStructure() {
        val source = readCustomToolDetailContentSource()

        assertEquals(3, source.countOccurrences("SettingExpandableTextItem("))
        assertEquals(1, source.countOccurrences("SettingToggleItem("))
        assertTrue(source.contains("SettingsItemDivider()"))
        assertTrue(source.contains("CustomToolEditableField.Name"))
        assertTrue(source.contains("CustomToolEditableField.Description"))
        assertTrue(source.contains("CustomToolEditableField.Command"))
    }

    @Test
    fun customToolDetailContent_usesConfirmedStringKeys() {
        val source = readCustomToolDetailContentSource()

        assertTrue(source.contains("R.string.custom_tool_editor_description"))
        assertTrue(source.contains("R.string.custom_tool_field_name"))
        assertTrue(source.contains("R.string.custom_tool_field_name_hint"))
        assertTrue(source.contains("R.string.custom_tool_field_description"))
        assertTrue(source.contains("R.string.custom_tool_field_description_hint"))
        assertTrue(source.contains("R.string.custom_tool_field_enabled"))
        assertTrue(source.contains("R.string.custom_tool_field_command"))
        assertTrue(source.contains("R.string.custom_tool_field_command_hint"))
        assertTrue(source.contains("R.string.custom_tool_save_action"))
        assertTrue(source.contains("R.string.custom_tool_error_load_failed"))
        assertTrue(source.contains("R.string.custom_tool_error_save_failed"))
        assertTrue(source.contains("R.string.custom_tool_error_delete_failed"))
    }

    private fun readCustomToolDetailContentSource(): String {
        return File(
            "src/main/java/com/niki914/nexus/agentic/app/ui/nexus/content/CustomToolDetailContent.kt"
        ).readText()
    }

    private fun String.countOccurrences(needle: String): Int {
        return split(needle).size - 1
    }
}
