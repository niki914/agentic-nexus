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
            .filter { tool -> isEnabled(settings, tool.name) }
            .sortedBy { it.name }
    }

    private fun isEnabled(settings: LocalSettings, name: String): Boolean {
        val value = settings.builtinToolFlags?.get(name) ?: return false
        return when (value) {
            is JsonPrimitive -> value.booleanOrNull ?: false
            is JsonObject -> (value["enabled"] as? JsonPrimitive)?.booleanOrNull ?: false
            else -> false
        }
    }

    companion object {
        fun default(): BuiltinToolRegistry = BuiltinToolRegistry(
            listOf(
                CreateCommandToolBuiltin(),
            )
        )
    }
}
