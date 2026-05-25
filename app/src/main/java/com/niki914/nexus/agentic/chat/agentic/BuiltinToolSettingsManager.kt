package com.niki914.nexus.agentic.chat.agentic

import android.content.Context
import com.niki914.nexus.agentic.mod.LocalSettings
import com.niki914.nexus.agentic.mod.XService
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

data class BuiltinToolSettingItem(
    val name: String,
    val description: String,
    val enabled: Boolean,
)

class BuiltinToolSettingsManager(
    private val registry: BuiltinToolRegistry = BuiltinToolRegistry.default(),
) {
    fun list(settings: LocalSettings): List<BuiltinToolSettingItem> {
        return registry.all()
            .sortedBy { it.name }
            .map { tool ->
                BuiltinToolSettingItem(
                    name = tool.name,
                    description = tool.description,
                    enabled = isEnabled(settings, tool),
                )
            }
    }

    suspend fun setEnabled(
        context: Context,
        name: String,
        enabled: Boolean,
    ): BuiltinToolResult {
        return try {
            val settings = XService.getLocalSettings(context)
            val result = withEnabled(settings, name, enabled)
            if (!result.ok) {
                return result
            }
            XService.putLocalSettings(context, buildUpdatedSettings(settings, name, enabled))
            successResult(name = name, enabled = enabled, settings = null)
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) {
                throw throwable
            }
            BuiltinToolResult.failure(
                code = "SETTINGS_WRITE_FAILED",
                message = "Failed to update builtin tool settings.",
                hint = throwable.message.orEmpty(),
            )
        }
    }

    fun withEnabled(
        settings: LocalSettings,
        name: String,
        enabled: Boolean,
    ): BuiltinToolResult {
        if (registry.find(name) == null) {
            return BuiltinToolResult.failure(
                code = "UNKNOWN_BUILTIN_TOOL",
                message = "Unknown builtin tool: $name.",
                fieldErrors = mapOf("name" to "Builtin tool is not registered."),
            )
        }

        val updatedSettings = buildUpdatedSettings(settings, name, enabled)
        return successResult(name = name, enabled = enabled, settings = updatedSettings)
    }

    private fun successResult(
        name: String,
        enabled: Boolean,
        settings: LocalSettings?,
    ): BuiltinToolResult {
        val data = mutableMapOf<String, JsonElement>(
            "available_next_turn" to JsonPrimitive(true),
            "name" to JsonPrimitive(name),
            "enabled" to JsonPrimitive(enabled),
        )
        if (settings != null) {
            data["settings"] = settings.props
        }
        return BuiltinToolResult.success(
            message = "Builtin tool setting updated.",
            data = JsonObject(data),
        )
    }

    private fun isEnabled(settings: LocalSettings, tool: BuiltinTool): Boolean {
        val value = settings.builtinToolFlags?.get(tool.name) ?: return tool.defaultEnabled
        return when (value) {
            is JsonPrimitive -> value.booleanOrNull ?: tool.defaultEnabled
            is JsonObject -> (value["enabled"] as? JsonPrimitive)?.booleanOrNull ?: tool.defaultEnabled
            else -> tool.defaultEnabled
        }
    }

    private fun buildUpdatedSettings(
        settings: LocalSettings,
        name: String,
        enabled: Boolean,
    ): LocalSettings {
        val props = settings.props.toMutableMap()
        val flags = settings.builtinToolFlags?.toMutableMap()
            ?: mutableMapOf<String, JsonElement>()
        flags[name] = JsonPrimitive(enabled)
        props["builtin_tool_flags"] = JsonObject(flags)
        return LocalSettings(JsonObject(props))
    }
}
