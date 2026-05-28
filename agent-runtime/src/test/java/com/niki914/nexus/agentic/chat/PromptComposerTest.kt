package com.niki914.nexus.agentic.chat

import com.niki914.nexus.agentic.chat.agentic.PromptComposer
import com.niki914.nexus.agentic.chat.agentic.PromptComposerInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PromptComposerTest {

    @Test
    fun compose_keepsStableSectionOrderAndFiltersBlankSections() {
        val result = PromptComposer().compose(
            PromptComposerInput(
                baseSystemPrompt = "base",
                memorySections = listOf("memory", " "),
                toolSections = listOf("", "tool"),
                runtimeSections = listOf("runtime"),
            )
        )

        assertEquals(
            listOf("system", "memory_0", "tool_0", "runtime_0"),
            result.sections.map { it.title },
        )
        assertEquals("base\n\nmemory\n\ntool\n\nruntime", result.finalSystemPrompt)
        assertFalse(result.finalSystemPrompt.contains("\n\n\n"))
    }
}
