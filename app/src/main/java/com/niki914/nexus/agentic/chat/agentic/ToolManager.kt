package com.niki914.nexus.agentic.chat.agentic

import android.content.Context
import com.niki914.nexus.agentic.chat.LocalTool
import com.niki914.nexus.agentic.chat.McpCachedTool
import com.niki914.nexus.agentic.chat.McpServerDefinition
import com.niki914.nexus.agentic.chat.ResolvedTools
import com.niki914.nexus.agentic.chat.agentic.buildin.BuiltinTool
import com.niki914.nexus.agentic.chat.agentic.buildin.BuiltinToolRegistry
import com.niki914.nexus.agentic.mod.LocalSettings
import com.niki914.nexus.agentic.repo.BuiltinToolSetting
import com.niki914.nexus.agentic.repo.CustomTool
import com.niki914.nexus.agentic.repo.LocalSettingsCodec
import com.niki914.nexus.agentic.repo.McpServer
import com.niki914.nexus.agentic.repo.McpTool
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

class ToolManager(
    private val builtinToolRegistry: BuiltinToolRegistry = BuiltinToolRegistry.default(),
) {
    suspend fun resolve(
        context: Context,
        settings: LocalSettings,
    ): ResolvedTools = resolve(settings)

    fun resolve(settings: LocalSettings): ResolvedTools {
        val mcpServers = LocalSettingsCodec.parseMcpServers(settings)
        return resolve(
            customTools = LocalSettingsCodec.parseCustomTools(settings),
            mcpServers = mcpServers,
            builtinSettings = buildBuiltinSettings(settings),
            mcpCachedTools = mcpServers.associate { server ->
                server.name to LocalSettingsCodec.parseMcpCache(settings, server)
            },
        )
    }

    fun resolve(
        customTools: List<CustomTool>,
        mcpServers: List<McpServer>,
        builtinSettings: List<BuiltinToolSetting>,
        mcpCachedTools: Map<String, List<McpTool>> = emptyMap(),
    ): ResolvedTools {
        val builtinTools = buildBuiltinTools(builtinSettings)
        val customRuntimeTools = buildCustomTools(customTools)
        val mcpRuntimeServers = buildMcpServers(
            servers = mcpServers,
            cachedTools = mcpCachedTools,
        )

        return ResolvedTools(
            builtinTools = builtinTools,
            customTools = customRuntimeTools,
            mcpServers = mcpRuntimeServers,
            promptLines = buildPromptLines(
                builtinTools = builtinTools,
                customTools = customRuntimeTools,
                mcpServers = mcpRuntimeServers,
            ),
        )
    }

    private fun buildBuiltinSettings(settings: LocalSettings): List<BuiltinToolSetting> {
        val flags = LocalSettingsCodec.parseBuiltinFlags(settings)
        return builtinToolRegistry.all()
            .sortedBy { it.name }
            .map { tool ->
                BuiltinToolSetting(
                    name = tool.name,
                    description = tool.description,
                    enabled = flags[tool.name]
                        ?: flags[tool::class.simpleName]
                        ?: tool.defaultEnabled,
                )
            }
    }

    private fun buildBuiltinTools(settings: List<BuiltinToolSetting>): List<LocalTool.Builtin> {
        return settings
            .filter { it.enabled }
            .sortedBy { it.name }
            .mapNotNull { setting ->
                val tool = findBuiltinTool(setting.name) ?: return@mapNotNull null
                LocalTool.Builtin(
                    name = setting.name,
                    description = setting.description,
                    tool = tool,
                )
            }
    }

    private fun findBuiltinTool(name: String): BuiltinTool? {
        return builtinToolRegistry.find(name)
            ?: builtinToolRegistry.all().firstOrNull { it::class.simpleName == name }
    }

    private fun buildCustomTools(tools: List<CustomTool>): List<LocalTool.Custom> {
        return tools
            .filter { it.enabled }
            .map { tool ->
                LocalTool.Custom(
                    name = tool.name,
                    description = tool.description,
                    enabled = tool.enabled,
                    command = tool.command,
                )
            }
            .associateBy(LocalTool.Custom::name)
            .values
            .toList()
    }

    private fun buildMcpServers(
        servers: List<McpServer>,
        cachedTools: Map<String, List<McpTool>>,
    ): List<McpServerDefinition> {
        return servers.map { server ->
            McpServerDefinition.Http(
                name = server.name,
                url = server.url,
                enabled = server.enabled,
                headers = server.headers,
                cachedTools = cachedTools[server.name].orEmpty().map(::toCachedTool),
            )
        }
    }

    private fun toCachedTool(tool: McpTool): McpCachedTool {
        return McpCachedTool(
            name = tool.name,
            description = tool.description,
            inputSchema = tool.inputSchemaJson.asJsonObjectOrEmpty(),
        )
    }

    private fun String.asJsonObjectOrEmpty(): JsonObject {
        if (isBlank()) {
            return JsonObject(emptyMap())
        }
        return try {
            Json.parseToJsonElement(this).jsonObject
        } catch (_: SerializationException) {
            JsonObject(emptyMap())
        } catch (_: IllegalArgumentException) {
            JsonObject(emptyMap())
        }
    }

    fun buildPromptLines(
        builtinTools: List<LocalTool>,
        customTools: List<LocalTool>,
        mcpServers: List<McpServerDefinition>,
    ): List<String> {
        val lines = mutableListOf<String>()
        val builtinToolNames = builtinTools.map { it.name }
        if (builtinToolNames.isNotEmpty()) {
            lines += "Available builtin tools: ${builtinToolNames.joinToString()}"
        }

        val customToolNames = customTools
            .filterIsInstance<LocalTool.Custom>()
            .map { it.name }
        if (customToolNames.isNotEmpty()) {
            lines += "Available custom tools: ${customToolNames.joinToString()}"
        }

        val enabledMcpServers = mcpServers.filter { it.enabled }
        if (enabledMcpServers.isNotEmpty()) {
            val serverNames = enabledMcpServers.joinToString { it.name }
            lines += "Available MCP servers: $serverNames"
        }

        return lines
    }

}
