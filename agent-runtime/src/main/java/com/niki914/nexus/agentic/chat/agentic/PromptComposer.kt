package com.niki914.nexus.agentic.chat.agentic

import com.niki914.nexus.agentic.chat.McpServerDefinition
import com.niki914.nexus.agentic.chat.ResolvedTools
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeSkillMetadata
import com.niki914.s3ss10n.McpDiscoverySnapshot
import com.niki914.s3ss10n.McpDiscoveryState
import com.niki914.s3ss10n.McpServerDiscoverySnapshot

data class PromptComposeResult(
    val finalSystemPrompt: String,
    val sections: List<PromptSection>,
)

data class PromptSection(
    val title: String,
    val content: String,
)

data class PromptComposerInput(
    val additionalInstructions: String,
    val memoryItems: List<String> = emptyList(),
    val tools: ResolvedTools = ResolvedTools(),
    val mcpDiscoverySnapshot: McpDiscoverySnapshot? = null,
    val enabledSkills: List<RuntimeSkillMetadata> = emptyList(),
)

class PromptComposer {

    fun compose(input: PromptComposerInput): PromptComposeResult {
        val sections = listOfNotNull(
            renderMemorySection(input.memoryItems),
            renderToolContextSection(input.tools, input.mcpDiscoverySnapshot),
            renderSkillContextSection(input.enabledSkills),
            renderAdditionalInstructions(input.additionalInstructions),
        )

        return PromptComposeResult(
            finalSystemPrompt = (listOf("# System Prompt") + sections.map { it.content.trim() })
                .joinToString(separator = "\n\n")
                .trim(),
            sections = sections,
        )
    }

    private fun renderMemorySection(items: List<String>): PromptSection? {
        val normalizedItems = items.map(String::trim).filter(String::isNotBlank)
        if (normalizedItems.isEmpty()) {
            return null
        }
        return PromptSection(
            title = "Agent Memory",
            content = buildString {
                appendLine("## Agent Memory")
                appendLine()
                appendLine("<memory>")
                normalizedItems.forEach { item -> appendLine("- $item") }
                append("</memory>")
            },
        )
    }

    private fun renderToolContextSection(
        tools: ResolvedTools,
        snapshot: McpDiscoverySnapshot?,
    ): PromptSection? {
        val blocks = listOfNotNull(
            renderNameBlock("builtin_tools", tools.builtinTools.map { it.name }),
            renderNameBlock("custom_tools", tools.customTools.map { it.name }),
            renderMcpServers(tools, snapshot),
        )
        if (blocks.isEmpty()) {
            return null
        }
        return PromptSection(
            title = "Tool Context",
            content = buildString {
                appendLine("## Tool Context")
                appendLine()
                append(blocks.joinToString(separator = "\n\n"))
            },
        )
    }

    private fun renderNameBlock(tag: String, names: List<String>): String? {
        val normalizedNames = names
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .sorted()
        if (normalizedNames.isEmpty()) {
            return null
        }
        return normalizedNames.joinToString(
            separator = "\n",
            prefix = "<$tag>\n",
            postfix = "\n</$tag>",
        ) { name -> "- $name" }
    }

    private fun renderMcpServers(
        tools: ResolvedTools,
        snapshot: McpDiscoverySnapshot?,
    ): String? {
        val enabledServers = tools.mcpServers
            .filter(McpServerDefinition::enabled)
            .map { it.name.trim() }
            .filter(String::isNotBlank)
            .distinct()
            .sorted()
        if (enabledServers.isEmpty()) {
            return null
        }
        val snapshotByName = snapshot?.servers?.values.orEmpty().associateBy { it.serverName }
        return enabledServers.joinToString(
            separator = "\n",
            prefix = "<mcp_servers>\n",
            postfix = "\n</mcp_servers>",
        ) { serverName ->
            "- ${snapshotByName[serverName]?.let(::renderMcpStatus) ?: "$serverName: idle"}"
        }
    }

    private fun renderMcpStatus(server: McpServerDiscoverySnapshot): String =
        when (server.state) {
            McpDiscoveryState.Available ->
                "${server.serverName}: loaded ${server.discoveredToolCount} tools"

            McpDiscoveryState.Discovering ->
                "${server.serverName}: loading"

            McpDiscoveryState.Failed -> {
                val message = server.errorMessage?.trim().takeUnless { it.isNullOrBlank() }
                if (message == null) {
                    "${server.serverName}: failed"
                } else {
                    "${server.serverName}: failed, msg: $message"
                }
            }

            McpDiscoveryState.UsingStaleCache ->
                "${server.serverName}: using cached ${server.discoveredToolCount} tools"

            McpDiscoveryState.Idle ->
                "${server.serverName}: idle"
        }

    private fun renderSkillContextSection(skills: List<RuntimeSkillMetadata>): PromptSection? {
        val lines = skills
            .mapNotNull { skill ->
                val id = skill.id.trim()
                if (id.isBlank()) {
                    null
                } else {
                    val name = skill.name.trim().ifBlank { id }
                    val description = skill.description.trim()
                    val absolutePath = skill.absolutePath.trim()
                    SkillContextLine(id, name, description, absolutePath)
                }
            }
            .sortedBy(SkillContextLine::id)
            .map { line ->
                buildString {
                    append("- ${line.id}: ${line.name}")
                    if (line.description.isNotBlank()) {
                        append(" - ${line.description}")
                    }
                    if (line.absolutePath.isNotBlank()) {
                        append(" [path: ${line.absolutePath}]")
                    }
                }
            }
        if (lines.isEmpty()) {
            return null
        }
        return PromptSection(
            title = "Skill Context",
            content = buildString {
                appendLine("## Skill Context")
                appendLine()
                appendLine("Skills are on-demand instruction sets. When the user request matches a skill's description below, call load_skill immediately — you DO NOT need the consent to use this tool. Each skill's absolute file path is listed; use it to locate companion scripts or reference files in the same directory. Note: this is an Android runtime without Python or other desktop interpreters, so companion scripts may not be able to execute.")
                appendLine()
                appendLine("<available_skills>")
                lines.forEach(::appendLine)
                append("</available_skills>")
            },
        )
    }

    private fun renderAdditionalInstructions(text: String): PromptSection? {
        val normalizedText = text.trim()
        if (normalizedText.isEmpty()) {
            return null
        }
        return PromptSection(
            title = "Additional instructions",
            content = buildString {
                appendLine("## Additional instructions")
                appendLine()
                append(normalizedText)
            },
        )
    }

    private data class SkillContextLine(
        val id: String,
        val name: String,
        val description: String,
        val absolutePath: String,
    )
}
