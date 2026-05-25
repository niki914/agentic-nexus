package com.niki914.nexus.agentic.chat.agentic

import com.niki914.s3ss10n.LocalToolConfig
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

class CreateCustomToolBuiltin(
    private val manager: CustomToolManager = CustomToolManager(),
) : BuiltinTool() {
    override val name: String = "create_custom_tool"

    override val description: String = "Create or update a custom tool in LocalSettings.custom_tools."

    override fun configure(config: LocalToolConfig) {
        config.description = description
        config.string("name") {
            description = "Unique custom tool name matching ^[a-zA-Z_][a-zA-Z0-9_]{1,63}$."
            required = true
        }
        config.string("description") {
            description = "Human-readable description shown to the model and UI."
            required = true
        }
        config.string("command") {
            description = "Shell command executed by the custom tool."
            required = true
        }
        config.boolean("enabled") {
            description = "Whether the created custom tool is enabled. Defaults to false."
            required = false
        }
        config.boolean("overwrite") {
            description = "Whether to replace an existing custom tool with the same name. Defaults to false."
            required = false
        }
        config.rawJsonSchema(CREATE_CUSTOM_TOOL_SCHEMA)
    }

    override suspend fun invoke(request: BuiltinToolRequest): BuiltinToolResult {
        val createRequest = try {
            parseArguments(request.argumentsJson)
        } catch (throwable: Throwable) {
            if (throwable is kotlinx.coroutines.CancellationException) {
                throw throwable
            }
            return BuiltinToolResult.failure(
                code = "INVALID_ARGUMENTS_JSON",
                message = "create_custom_tool arguments must be a JSON object with name, description, command, enabled, and overwrite fields.",
                hint = """Example: {"name":"battery_status","description":"Read current battery status.","command":"dumpsys battery","enabled":false,"overwrite":false}""",
                fieldErrors = mapOf("argumentsJson" to (throwable.message ?: "Invalid JSON object.")),
            )
        }

        return manager.createOrUpdate(request.context, createRequest)
    }

    private fun parseArguments(argumentsJson: String): CustomToolCreateRequest {
        val element = try {
            Json.parseToJsonElement(argumentsJson)
        } catch (throwable: SerializationException) {
            throw IllegalArgumentException("argumentsJson is not valid JSON.", throwable)
        } catch (throwable: IllegalArgumentException) {
            throw IllegalArgumentException("argumentsJson is not valid JSON.", throwable)
        }
        val obj = element as? JsonObject
            ?: throw IllegalArgumentException("argumentsJson must be a JSON object.")
        return CustomToolCreateRequest(
            name = obj.string("name"),
            description = obj.string("description"),
            command = obj.string("command"),
            enabled = obj.boolean("enabled", default = false),
            overwrite = obj.boolean("overwrite", default = false),
        )
    }

    private fun JsonObject.string(key: String): String {
        return this[key]?.jsonPrimitive?.contentOrNull.orEmpty()
    }

    private fun JsonObject.boolean(key: String, default: Boolean): Boolean {
        return (this[key] as? JsonPrimitive)?.booleanOrNull ?: default
    }

    companion object {
        private const val CREATE_CUSTOM_TOOL_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "name": {
                  "type": "string",
                  "description": "Unique custom tool name matching ^[a-zA-Z_][a-zA-Z0-9_]{1,63}$."
                },
                "description": {
                  "type": "string",
                  "description": "Human-readable description shown to the model and UI."
                },
                "command": {
                  "type": "string",
                  "description": "Shell command executed by the custom tool."
                },
                "enabled": {
                  "type": "boolean",
                  "description": "Whether the created custom tool is enabled.",
                  "default": false
                },
                "overwrite": {
                  "type": "boolean",
                  "description": "Whether to replace an existing custom tool with the same name.",
                  "default": false
                }
              },
              "required": ["name", "description", "command"]
            }
        """
    }
}
