package com.niki914.nexus.agentic.chat.agentic.buildin

import android.content.Context
import com.niki914.nexus.agentic.mod.LocalSettings
import com.niki914.nexus.agentic.repo.BuiltinToolSetting
import com.niki914.nexus.agentic.repo.XRepo
import kotlinx.coroutines.CancellationException
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
    suspend fun load(context: Context): List<BuiltinToolSettingItem> {
        XRepo.init(context)
        return XRepo.builtinTools.list().map { it.toItem() }
    }

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
            XRepo.init(context)
            val validation = XRepo.builtinTools.setEnabled(name, enabled)
            if (validation != null) {
                return BuiltinToolResult.failure(
                    code = "UNKNOWN_BUILTIN_TOOL",
                    message = "Unknown builtin tool: $name.",
                    fieldErrors = mapOf(validation.field to validation.message),
                )
            }
            successResult(name = name, enabled = enabled)
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

    private fun successResult(
        name: String,
        enabled: Boolean,
    ): BuiltinToolResult {
        val data = JsonObject(
            mapOf(
                "available_next_turn" to JsonPrimitive(true),
                "name" to JsonPrimitive(name),
                "enabled" to JsonPrimitive(enabled),
            )
        )
        return BuiltinToolResult.success(
            message = "Builtin tool setting updated.",
            data = data,
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

    private fun BuiltinToolSetting.toItem(): BuiltinToolSettingItem {
        return BuiltinToolSettingItem(
            name = name,
            description = description,
            enabled = enabled,
        )
    }
}
