package com.niki914.nexus.agentic.app.ui.nexus

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class NexusAppSourceTest {
    @Test
    fun nexusApp_routesBackThroughCurrentChromeHandlerBeforeFallback() {
        val source = readNexusAppSource()

        assertTrue(source.contains("fun requestBack()"))
        assertTrue(source.contains("val backHandler = currentChrome.backHandler"))
        assertTrue(source.contains("backHandler.shouldConsumeBack()"))
        assertTrue(source.contains("backHandler.onConsumeBack()"))
        assertTrue(source.contains("popOrMoveTaskToBack()"))
        assertTrue(source.contains("bindAction(currentLeftAction, fallback = ::requestBack)"))
    }

    @Test
    fun nexusApp_keepsChromeMenuBackPriority() {
        val source = readNexusAppSource()

        assertTrue(source.contains("if (chromeMenuExpanded)"))
        assertTrue(source.contains("closeChromeMenu()"))
        assertTrue(source.contains("requestBack()"))
    }

    private fun readNexusAppSource(): String {
        return File("src/main/java/com/niki914/nexus/agentic/app/ui/nexus/NexusApp.kt").readText()
    }
}
