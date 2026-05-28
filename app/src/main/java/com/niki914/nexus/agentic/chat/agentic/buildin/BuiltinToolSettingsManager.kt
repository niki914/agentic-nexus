package com.niki914.nexus.agentic.chat.agentic.buildin

import com.niki914.nexus.agentic.repo.BuiltinToolSetting
import com.niki914.nexus.agentic.repo.XRepo
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

data class BuiltinToolSettingItem(
    val name: String,
    val description: String,
    val enabled: Boolean,
)

class BuiltinToolSettingsManager {
    suspend fun load(): List<BuiltinToolSettingItem> {
        return XRepo.builtinTools.list().map { it.toItem() }
    }

    suspend fun setEnabled(
        name: String,
        enabled: Boolean,
    ): BuiltinToolResult {
        return try {
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

    private fun BuiltinToolSetting.toItem(): BuiltinToolSettingItem {
        return BuiltinToolSettingItem(
            name = name,
            description = description,
            enabled = enabled,
        )
    }
}
