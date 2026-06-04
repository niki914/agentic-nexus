package com.niki914.nexus.agentic.app.ui.nexus.content

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DialogBackHandlerSourceTest {
    @Test
    fun memorySettingsContent_consumesBackWhenEditDialogVisible() {
        val source = File(
            "src/main/java/com/niki914/nexus/agentic/app/ui/nexus/content/MemorySettingsContent.kt"
        ).readText()

        assertTrue(source.contains("import androidx.compose.runtime.rememberUpdatedState"))
        assertTrue(source.contains("import com.niki914.nexus.agentic.app.ui.nexus.PageBackHandler"))
        assertTrue(source.contains("val latestUiState by rememberUpdatedState(uiState)"))
        assertTrue(source.contains("val latestViewModel by rememberUpdatedState(viewModel)"))
        assertTrue(source.contains("backHandler = PageBackHandler("))
        assertTrue(source.contains("latestUiState.editingDialog != null"))
        assertTrue(source.contains("MemorySettingsIntent.DismissEditDialog"))
    }

}
