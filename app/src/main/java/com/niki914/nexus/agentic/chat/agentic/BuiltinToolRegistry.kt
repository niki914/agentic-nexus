package com.niki914.nexus.agentic.chat.agentic

import com.niki914.nexus.agentic.mod.LocalSettings
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

class BuiltinToolRegistry(
    private val tools: List<BuiltinTool>,
) {
    fun all(): List<BuiltinTool> = tools

    fun find(name: String): BuiltinTool? {
        return tools.firstOrNull { it.name == name }
    }

    fun resolveEnabled(settings: LocalSettings): List<BuiltinTool> {
        return tools
            .filter { tool -> isEnabled(settings, tool) }
            .sortedBy { it.name }
    }

    private fun isEnabled(settings: LocalSettings, tool: BuiltinTool): Boolean {
        val value = settings.builtinToolFlags?.get(tool.name) ?: return tool.defaultEnabled
        return when (value) {
            is JsonPrimitive -> value.booleanOrNull ?: tool.defaultEnabled
            is JsonObject -> (value["enabled"] as? JsonPrimitive)?.booleanOrNull ?: tool.defaultEnabled
            else -> tool.defaultEnabled
        }
    }

    companion object {
        fun default(): BuiltinToolRegistry = BuiltinToolRegistry(
            listOf(
                CreateCustomToolBuiltin(),
                RunCommandBuildin_WIP_SAFE(),
            )
        )
    }
}
