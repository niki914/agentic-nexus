package com.niki914.nexus.agentic.chat.agentic

data class PromptComposeResult(
    val finalSystemPrompt: String,
    val sections: List<PromptSection>,
)

data class PromptSection(
    val title: String,
    val content: String,
)

data class PromptXMLSection(
    val title: String,
    val tag: String,
    val items: List<String>,
)

data class PromptComposerInput(
    val baseSystemPrompt: String,
    val memoryItems: List<String> = emptyList(),
    val toolSections: List<String> = emptyList(),
    val runtimeSections: List<String> = emptyList(),
)

class PromptComposer {

    fun compose(input: PromptComposerInput): PromptComposeResult {
        val sections = buildList {
            add(
                PromptSection(
                    title = "Core system instructions",
                    content = input.baseSystemPrompt.ifBlank { DEFAULT_SYSTEM_PROMPT },
                )
            )
            addXmlSection(
                PromptXMLSection(
                    title = "Persistent memory",
                    tag = "memory",
                    items = input.memoryItems,
                )
            )
            input.toolSections
                .filter { it.isNotBlank() }
                .forEachIndexed { index, text ->
                    add(PromptSection(title = "Available tool instructions ${index + 1}", content = text))
                }
            input.runtimeSections
                .filter { it.isNotBlank() }
                .forEachIndexed { index, text ->
                    add(PromptSection(title = "Runtime context ${index + 1}", content = text))
                }
        }

        return PromptComposeResult(
            finalSystemPrompt = sections.joinToString(separator = "\n\n") { it.content.trim() }
                .trim(),
            sections = sections,
        )
    }

    companion object {
        private const val DEFAULT_SYSTEM_PROMPT = "You are a helpful assistant."
    }
}

private fun MutableList<PromptSection>.addXmlSection(section: PromptXMLSection) {
    val items = section.items.map(String::trim).filter(String::isNotBlank)
    if (items.isEmpty()) {
        return
    }
    add(
        PromptSection(
            title = section.title,
            content = items.joinToString(
                separator = "\n",
                prefix = "<${section.tag}>\n",
                postfix = "\n</${section.tag}>",
            ),
        )
    )
}
