package com.niki914.nexus.agentic.chat.agentic

import android.content.Context
import com.niki914.nexus.agentic.mod.LocalSettings
import com.niki914.nexus.agentic.mod.XService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

data class CustomToolCreateRequest(
    val name: String,
    val description: String,
    val command: String,
    val enabled: Boolean = false,
    val overwrite: Boolean = false,
)

data class CustomToolConfig(
    val name: String,
    val description: String,
    val enabled: Boolean,
    val command: String,
)

class CustomToolManager(
    private val reservedToolNames: () -> Set<String> = {
        BuiltinToolRegistry.default().all().map { it.name }.toSet()
    },
    private val safetyPolicy: ShellCommandSafetyPolicy = ShellCommandSafetyPolicy(),
) {
    suspend fun createOrUpdate(
        context: Context,
        request: CustomToolCreateRequest,
    ): BuiltinToolResult {
        return persist(context) { settings ->
            val existingItems = parseCustomTools(settings)
            val validationError = validate(
                request = request,
                existingNames = existingItems.map { it.name }.toSet(),
                reservedNames = reservedNames(),
            )
            if (validationError != null) {
                return@persist validationError to null
            }

            val normalized = request.toConfig()
            val updatedItems = if (existingItems.any { it.name == normalized.name }) {
                existingItems.map { item ->
                    if (item.name == normalized.name) normalized else item
                }
            } else {
                existingItems + normalized
            }

            successForTool(normalized) to updatedItems
        }
    }

    suspend fun saveAll(
        context: Context,
        items: List<CustomToolConfig>,
    ): BuiltinToolResult {
        return persist(context) {
            val normalizedItems = items.map { it.normalized() }
            val validationError = validateAll(
                items = normalizedItems,
                reservedNames = reservedNames(),
            )
            if (validationError != null) {
                return@persist validationError to null
            }

            BuiltinToolResult.success(
                message = "Custom tools were saved.",
                hint = "The updated custom tools are available after the next runtime refresh.",
                data = JsonObject(
                    mapOf(
                        "available_next_turn" to JsonPrimitive(true),
                        "count" to JsonPrimitive(normalizedItems.size),
                    )
                ),
            ) to normalizedItems
        }
    }

    fun validate(
        request: CustomToolCreateRequest,
        existingNames: Set<String>,
        reservedNames: Set<String>,
    ): BuiltinToolResult? {
        val normalized = request.normalized()
        val fieldErrors = mutableMapOf<String, String>()

        if (normalized.name.isBlank()) {
            fieldErrors["name"] = "Required field 'name' is missing."
        } else if (!NAME_PATTERN.matches(normalized.name)) {
            return BuiltinToolResult.failure(
                code = "INVALID_NAME",
                message = "Custom tool name '${normalized.name}' is invalid.",
                hint = "Use 2-64 characters matching ^[a-zA-Z_][a-zA-Z0-9_]{1,63}$, for example battery_status.",
                fieldErrors = mapOf("name" to "Name must start with a letter or underscore and contain only letters, digits, or underscores."),
            )
        }

        if (normalized.description.isBlank()) {
            fieldErrors["description"] = "Required field 'description' is missing."
        }
        if (normalized.command.isBlank()) {
            fieldErrors["command"] = "Required field 'command' is missing."
        }
        if (fieldErrors.isNotEmpty()) {
            return BuiltinToolResult.failure(
                code = "MISSING_REQUIRED_FIELD",
                message = "Custom tool requires non-empty name, description, and command.",
                hint = "Provide all required fields before creating the custom tool.",
                fieldErrors = fieldErrors,
            )
        }

        if (normalized.name in reservedNames) {
            return BuiltinToolResult.failure(
                code = "RESERVED_NAME",
                message = "Custom tool name '${normalized.name}' is reserved by a builtin tool.",
                hint = "Choose a different name that does not conflict with builtin tools.",
                fieldErrors = mapOf("name" to "Reserved builtin tool name."),
            )
        }
        if (normalized.name in existingNames && !normalized.overwrite) {
            return BuiltinToolResult.failure(
                code = "NAME_CONFLICT",
                message = "Custom tool name '${normalized.name}' already exists.",
                hint = "Use a different name, or set overwrite=true if replacement is intended.",
                fieldErrors = mapOf("name" to "Already exists in custom_tools."),
            )
        }
        val decision = safetyPolicy.evaluate(normalized.command)
        if (!decision.allowed) {
            return BuiltinToolResult.failure(
                code = decision.code,
                message = "Custom tool '${normalized.name}' uses a command blocked by the basic safety policy.",
                hint = "Remove high-risk operations such as rm -rf, reboot, su, setprop, pm uninstall, or dd.",
                fieldErrors = mapOf("command" to "Unsafe command pattern was rejected."),
            )
        }

        return null
    }

    private suspend fun persist(
        context: Context,
        build: (LocalSettings) -> Pair<BuiltinToolResult, List<CustomToolConfig>?>,
    ): BuiltinToolResult {
        return writeMutex.withLock {
            try {
                val latestSettings = XService.getLocalSettings(context)
                val (result, updatedItems) = build(latestSettings)
                if (updatedItems == null) {
                    return@withLock result
                }

                val updatedProps = latestSettings.props.toMutableMap()
                updatedProps[CUSTOM_TOOLS_KEY] = buildCustomToolsJson(updatedItems)
                XService.putLocalSettings(context, LocalSettings(JsonObject(updatedProps)))
                result
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) {
                    throw throwable
                }
                BuiltinToolResult.failure(
                    code = "SETTINGS_WRITE_FAILED",
                    message = "Failed to write LocalSettings.custom_tools: ${throwable.message ?: throwable::class.java.simpleName}.",
                    hint = "Retry after confirming the settings provider is available.",
                )
            }
        }
    }

    private fun parseCustomTools(settings: LocalSettings): List<CustomToolConfig> {
        return settings.customTools
            ?.mapNotNull { element ->
                val obj = element as? JsonObject ?: return@mapNotNull null
                CustomToolConfig(
                    name = obj.string("name").trim(),
                    description = obj.string("description").trim(),
                    enabled = obj.boolean("enabled", default = true),
                    command = obj.string("command").trim(),
                )
            }
            ?: emptyList()
    }

    private fun buildCustomToolsJson(items: List<CustomToolConfig>): JsonArray {
        return JsonArray(
            items.map { item ->
                JsonObject(
                    mapOf(
                        "name" to JsonPrimitive(item.name),
                        "description" to JsonPrimitive(item.description),
                        "enabled" to JsonPrimitive(item.enabled),
                        "command" to JsonPrimitive(item.command),
                    )
                )
            }
        )
    }

    private fun validateAll(
        items: List<CustomToolConfig>,
        reservedNames: Set<String>,
    ): BuiltinToolResult? {
        val names = mutableSetOf<String>()
        items.forEach { item ->
            val request = CustomToolCreateRequest(
                name = item.name,
                description = item.description,
                command = item.command,
                enabled = item.enabled,
                overwrite = false,
            )
            val validationError = validate(
                request = request,
                existingNames = emptySet(),
                reservedNames = reservedNames,
            )
            if (validationError != null) {
                return validationError
            }
            if (!names.add(item.name)) {
                return BuiltinToolResult.failure(
                    code = "NAME_CONFLICT",
                    message = "Custom tool name '${item.name}' appears more than once.",
                    hint = "Each custom tool name must be unique.",
                    fieldErrors = mapOf("name" to "Duplicate name in custom_tools."),
                )
            }
        }
        return null
    }

    private fun successForTool(
        tool: CustomToolConfig,
    ): BuiltinToolResult {
        return BuiltinToolResult.success(
            message = "Custom tool '${tool.name}' was saved.",
            hint = "The tool is available after the next runtime refresh.",
            data = JsonObject(
                mapOf(
                    "available_next_turn" to JsonPrimitive(true),
                    "tool" to JsonObject(
                        mapOf(
                            "name" to JsonPrimitive(tool.name),
                            "enabled" to JsonPrimitive(tool.enabled),
                        )
                    ),
                )
            ),
        )
    }

    private fun reservedNames(): Set<String> {
        return reservedToolNames()
    }

    private fun CustomToolCreateRequest.normalized(): CustomToolCreateRequest {
        return copy(
            name = name.trim(),
            description = description.trim(),
            command = command.trim(),
        )
    }

    private fun CustomToolCreateRequest.toConfig(): CustomToolConfig {
        val normalized = normalized()
        return CustomToolConfig(
            name = normalized.name,
            description = normalized.description,
            enabled = normalized.enabled,
            command = normalized.command,
        )
    }

    private fun CustomToolConfig.normalized(): CustomToolConfig {
        return copy(
            name = name.trim(),
            description = description.trim(),
            command = command.trim(),
        )
    }

    private fun JsonObject.string(key: String): String {
        return (this[key] as? JsonPrimitive)?.contentOrNull.orEmpty()
    }

    private fun JsonObject.boolean(key: String, default: Boolean): Boolean {
        return (this[key] as? JsonPrimitive)?.booleanOrNull ?: default
    }

    companion object {
        private const val CUSTOM_TOOLS_KEY = "custom_tools"
        private val NAME_PATTERN = Regex("^[a-zA-Z_][a-zA-Z0-9_]{1,63}$")
        private val writeMutex = Mutex()
    }
}
