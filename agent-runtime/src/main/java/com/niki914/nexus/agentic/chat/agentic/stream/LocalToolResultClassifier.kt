package com.niki914.nexus.agentic.chat.agentic.stream

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

object LocalToolResultClassifier {
    fun failureMessage(resultJson: String?): String? {
        val result = resultJson
            ?.takeIf { it.isNotBlank() }
            ?.let(::parseJsonObject)
            ?: return null

        result.structuredErrorMessage()?.let { return it }

        val explicitOk = result["ok"]?.jsonPrimitive?.booleanOrNull
        if (explicitOk == false) {
            return result.statusMessage() ?: "Tool returned ok=false."
        }

        val exitCode = result["exit_code"]
            ?.jsonPrimitive
            ?.contentOrNull
            ?.toIntOrNull()
        if (exitCode != null && exitCode != 0) {
            return result.nonZeroExitMessage(exitCode)
        }

        return null
    }

    private fun parseJsonObject(value: String): JsonObject? {
        return runCatching { Json.parseToJsonElement(value) as? JsonObject }.getOrNull()
    }

    private fun JsonObject.structuredErrorMessage(): String? {
        val error = this["error"] as? JsonObject ?: return null
        val code = error["code"]
            ?.jsonPrimitive
            ?.contentOrNull
            ?.takeIf { it.isNotBlank() }
            ?: return null
        return error["message"]
            ?.jsonPrimitive
            ?.contentOrNull
            ?.takeIf { it.isNotBlank() }
            ?: code
    }

    private fun JsonObject.nonZeroExitMessage(exitCode: Int): String {
        return statusMessage() ?: "Command completed with non-zero exit code $exitCode."
    }

    private fun JsonObject.statusMessage(): String? {
        return listOf("stderr", "message", "code")
            .firstNotNullOfOrNull { key ->
                this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            }
    }
}
