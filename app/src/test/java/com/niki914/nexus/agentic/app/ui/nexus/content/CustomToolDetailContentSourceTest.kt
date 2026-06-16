package com.niki914.nexus.agentic.app.ui.nexus.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CustomToolDetailContentSourceTest {
    @Test
    fun customToolDetailContent_usesDetailScaffoldAndChromeBackContract() {
        val source = readCustomToolDetailContentSource()
        val sharedScaffoldSource = readEditableSettingsDetailScaffoldSource()

        assertTrue(source.contains("EditableSettingsDetailChrome("))
        assertTrue(source.contains("EditableSettingsDetailFormScaffold("))
        assertTrue(source.contains("CustomToolSettingsIntent.DeleteCurrent"))
        assertTrue(source.contains("requestedFocusField"))
        assertTrue(sharedScaffoldSource.contains("SettingsDetailFormScaffold("))
        assertTrue(sharedScaffoldSource.contains("RegisterPageChrome(pageChromeContribution)"))
        assertTrue(sharedScaffoldSource.contains("PageChromeContribution("))
        assertTrue(sharedScaffoldSource.contains("backHandler = PageBackHandler("))
        assertTrue(sharedScaffoldSource.contains("Icons.Default.Delete"))
        assertTrue(sharedScaffoldSource.contains("onBackgroundTap = fieldController.clearActiveField"))
    }

    @Test
    fun customToolDetailContent_routesBackThroughUnsavedChangesDialog() {
        val source = readCustomToolDetailContentSource()
        val sharedScaffoldSource = readEditableSettingsDetailScaffoldSource()

        assertTrue(source.contains("uiState.formState.hasUnsavedChanges"))
        assertTrue(source.contains("onDiscardChanges = onBack"))
        assertTrue(sharedScaffoldSource.contains("shouldConsumeBack = {"))
        assertTrue(sharedScaffoldSource.contains("latestHasUnsavedChanges()"))
        assertTrue(sharedScaffoldSource.contains("onConsumeBack = {"))
        assertTrue(sharedScaffoldSource.contains("showUnsavedChangesDialog = true"))
        assertTrue(sharedScaffoldSource.contains("ConfirmationLiquidDialog("))
        assertTrue(sharedScaffoldSource.contains("visible = showUnsavedChangesDialog"))
        assertTrue(sharedScaffoldSource.contains("negativeButtonText = stringResource(R.string.unsaved_changes_dialog_cancel)"))
        assertTrue(sharedScaffoldSource.contains("positiveButtonText = stringResource(R.string.unsaved_changes_dialog_confirm_exit)"))
        assertTrue(sharedScaffoldSource.contains("latestOnDiscardChanges()"))
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
        val sharedScaffoldSource = readEditableSettingsDetailScaffoldSource()

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
        assertTrue(sharedScaffoldSource.contains("R.string.unsaved_changes_dialog_title"))
        assertTrue(sharedScaffoldSource.contains("R.string.unsaved_changes_dialog_text"))
        assertTrue(sharedScaffoldSource.contains("R.string.unsaved_changes_dialog_confirm_exit"))
        assertTrue(sharedScaffoldSource.contains("R.string.unsaved_changes_dialog_cancel"))
    }

    private fun readCustomToolDetailContentSource(): String {
        return File(
            "src/main/java/com/niki914/nexus/agentic/app/ui/nexus/content/CustomToolDetailContent.kt"
        ).readText()
    }

    private fun readEditableSettingsDetailScaffoldSource(): String {
        return File(
            "src/main/java/com/niki914/nexus/agentic/app/ui/nexus/content/EditableSettingsDetailScaffold.kt"
        ).readText()
    }

    private fun String.countOccurrences(needle: String): Int {
        return split(needle).size - 1
    }
}
