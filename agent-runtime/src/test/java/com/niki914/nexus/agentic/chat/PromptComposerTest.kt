package com.niki914.nexus.agentic.chat

import com.niki914.nexus.agentic.chat.agentic.PromptComposer
import com.niki914.nexus.agentic.chat.agentic.PromptComposerInput
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeLlmConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PromptComposerTest {

    @Test
    fun compose_keepsStableSectionOrderAndFiltersBlankSections() {
        val result = PromptComposer().compose(
            PromptComposerInput(
                baseSystemPrompt = "base",
                memoryItems = listOf("memory", " "),
                toolSections = listOf("", "tool"),
                runtimeSections = listOf("runtime"),
            )
        )

        assertEquals(
            listOf(
                "Core system instructions",
                "Persistent memory",
                "Available tool instructions 1",
                "Runtime context 1",
            ),
            result.sections.map { it.title },
        )
        assertEquals("base\n\n<memory>\nmemory\n</memory>\n\ntool\n\nruntime", result.finalSystemPrompt)
        assertFalse(result.finalSystemPrompt.contains("\n\n\n"))
    }

    @Test
    fun llmController_prefersMemoriesOverMemoryPrompt() {
        val items = buildMemoryItems(
            RuntimeLlmConfig(
                memoryPrompt = "legacy",
                memories = listOf(" A ", "B", " "),
            )
        )

        assertEquals(listOf("A", "B"), items)
        assertFalse(items.contains("legacy"))
    }

    @Test
    fun compose_wrapsMemoryItemsInSingleXmlBlock() {
        val result = PromptComposer().compose(
            PromptComposerInput(
                baseSystemPrompt = "base",
                memoryItems = listOf(" A ", "B", " "),
            )
        )

        assertEquals(
            "<memory>\nA\nB\n</memory>",
            result.sections.single { it.title == "Persistent memory" }.content,
        )
        assertEquals("base\n\n<memory>\nA\nB\n</memory>", result.finalSystemPrompt)
    }

    private fun buildMemoryItems(config: RuntimeLlmConfig): List<String> {
        val method = LLMController::class.java.getDeclaredMethod(
            "buildMemoryItems",
            RuntimeLlmConfig::class.java,
        )
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return method.invoke(LLMController, config) as List<String>
    }
}
