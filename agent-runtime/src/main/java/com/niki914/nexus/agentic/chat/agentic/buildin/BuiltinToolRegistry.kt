package com.niki914.nexus.agentic.chat.agentic.buildin

import com.niki914.nexus.agentic.chat.agentic.buildin.impl.CreateCustomToolBuiltin
import com.niki914.nexus.agentic.chat.agentic.buildin.impl.NotifyBuiltin
import com.niki914.nexus.agentic.chat.agentic.buildin.impl.RunCommandBuildin_WIP_SAFE

class BuiltinToolRegistry(
    private val tools: List<BuiltinTool>,
) {
    fun all(): List<BuiltinTool> = tools

    fun find(name: String): BuiltinTool? {
        return tools.firstOrNull { it.name == name }
    }

    companion object {
        fun default(): BuiltinToolRegistry = BuiltinToolRegistry(
            listOf(
                CreateCustomToolBuiltin(),
                NotifyBuiltin(),
                RunCommandBuildin_WIP_SAFE(),
            )
        )
    }
}
