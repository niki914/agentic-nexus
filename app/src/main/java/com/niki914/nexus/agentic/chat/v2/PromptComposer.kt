package com.niki914.nexus.agentic.chat.v2

data class PromptComposeResult(
    val finalSystemPrompt: String,
    val sections: List<PromptSection>,
)

data class PromptSection(
    val title: String,
    val content: String,
)

data class PromptComposerInput(
    val baseSystemPrompt: String,
    val memorySections: List<String> = emptyList(),
    val toolSections: List<String> = emptyList(),
    val runtimeSections: List<String> = emptyList(),
)

class PromptComposer {

    fun compose(input: PromptComposerInput): PromptComposeResult {
        val sections = buildList {
            add(
                PromptSection(
                    title = "system",
                    content = input.baseSystemPrompt.ifBlank { DEFAULT_SYSTEM_PROMPT },
                )
            )
            input.memorySections
                .filter { it.isNotBlank() }
                .forEachIndexed { index, text ->
                    add(PromptSection(title = "memory_$index", content = text))
                }
            input.toolSections
                .filter { it.isNotBlank() }
                .forEachIndexed { index, text ->
                    add(PromptSection(title = "tool_$index", content = text))
                }
            input.runtimeSections
                .filter { it.isNotBlank() }
                .forEachIndexed { index, text ->
                    add(PromptSection(title = "runtime_$index", content = text))
                }
        }

        return PromptComposeResult(
            finalSystemPrompt = sections.joinToString(separator = "\n\n") { it.content.trim() }.trim(),
            sections = sections,
        )
    }

    companion object {
        private const val DEFAULT_SYSTEM_PROMPT = "You are a helpful assistant."
    }
}
