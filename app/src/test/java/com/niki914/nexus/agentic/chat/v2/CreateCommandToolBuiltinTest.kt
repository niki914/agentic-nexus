package com.niki914.nexus.agentic.chat.v2

import android.content.ContextWrapper
import com.niki914.nexus.agentic.chat.agentic.BuiltinToolRequest
import com.niki914.nexus.agentic.chat.agentic.CommandToolCreateRequest
import com.niki914.nexus.agentic.chat.agentic.CreateCommandToolBuiltin
import com.niki914.s3ss10n.LocalToolConfig
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateCommandToolBuiltinTest {
    @Test
    fun configure_registersDescriptionAndSchemaWithDisabledDefault() {
        val config = LocalToolConfig()

        CreateCommandToolBuiltin().configure(config)

        assertTrue(config.description.contains("Create or update a command tool"))
        val schema = Json.parseToJsonElement(config.rawInputSchemaJson!!).jsonObject
        val properties = schema["properties"]!!.jsonObject
        assertEquals(listOf("name", "description", "command"), schema["required"]!!.jsonArray.map { it.jsonPrimitive.content })
        assertEquals("string", properties["name"]!!.jsonObject["type"]!!.jsonPrimitive.content)
        assertEquals("string", properties["description"]!!.jsonObject["type"]!!.jsonPrimitive.content)
        assertEquals("string", properties["command"]!!.jsonObject["type"]!!.jsonPrimitive.content)
        assertEquals("boolean", properties["enabled"]!!.jsonObject["type"]!!.jsonPrimitive.content)
        assertFalse(properties["enabled"]!!.jsonObject["default"]!!.jsonPrimitive.boolean)
        assertEquals("boolean", properties["overwrite"]!!.jsonObject["type"]!!.jsonPrimitive.content)
        assertFalse(properties["overwrite"]!!.jsonObject["default"]!!.jsonPrimitive.boolean)
    }

    @Test
    fun parseArguments_readsRequiredFieldsAndBooleanDefaults() {
        val method = CreateCommandToolBuiltin::class.java.getDeclaredMethod(
            "parseArguments",
            String::class.java,
        )
        method.isAccessible = true

        val request = method.invoke(
            CreateCommandToolBuiltin(),
            """
            {
              "name": "battery_status",
              "description": "Read current battery status.",
              "command": "dumpsys battery"
            }
            """.trimIndent(),
        ) as CommandToolCreateRequest

        assertEquals("battery_status", request.name)
        assertEquals("Read current battery status.", request.description)
        assertEquals("dumpsys battery", request.command)
        assertFalse(request.enabled)
        assertFalse(request.overwrite)
    }

    @Test
    fun invoke_returnsInvalidArgumentsJsonForNonObjectArguments() = runTest {
        val result = CreateCommandToolBuiltin().invoke(
            BuiltinToolRequest(
                context = ContextWrapper(null),
                name = "create_command_tool",
                argumentsJson = "[]",
            )
        )

        assertFalse(result.ok)
        assertEquals("INVALID_ARGUMENTS_JSON", result.code)
        assertTrue(result.fieldErrors.containsKey("argumentsJson"))
    }
}
