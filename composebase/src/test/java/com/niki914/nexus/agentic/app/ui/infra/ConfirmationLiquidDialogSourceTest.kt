package com.niki914.nexus.agentic.app.ui.infra

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ConfirmationLiquidDialogSourceTest {
    @Test
    fun confirmationLiquidDialog_placesPositivePrimaryActionBeforeNegativeAction() {
        val source = File(
            "src/main/java/com/niki914/nexus/agentic/app/ui/infra/ConfirmationLiquidDialog.kt"
        ).readText()

        val positiveIndex = source.indexOf("text = positiveButtonText")
        val negativeIndex = source.indexOf("text = negativeButtonText")

        assertTrue(positiveIndex >= 0)
        assertTrue(negativeIndex >= 0)
        assertTrue(positiveIndex < negativeIndex)
        assertTrue(source.contains("containerColor = MaterialTheme.colorScheme.primary"))
        assertTrue(source.contains("containerColor = MaterialTheme.colorScheme.surfaceContainerHighest"))
    }
}
