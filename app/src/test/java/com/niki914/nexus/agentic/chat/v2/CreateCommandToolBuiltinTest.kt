package com.niki914.nexus.agentic.chat.v2

import android.content.Context
import android.content.ContextWrapper
import com.niki914.nexus.agentic.chat.agentic.buildin.BuiltinToolRequest
import com.niki914.nexus.agentic.chat.agentic.custom.CustomToolCreateRequest
import com.niki914.nexus.agentic.chat.agentic.buildin.impl.CreateCustomToolBuiltin
import com.niki914.nexus.agentic.mod.LocalSettings
import com.niki914.nexus.agentic.repo.LocalSettingsStore
import com.niki914.nexus.agentic.repo.XRepo
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeCustomTool as CustomTool
import com.niki914.s3ss10n.LocalToolConfig
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateCustomToolBuiltinTest {
    private val context: Context = object : ContextWrapper(null) {
        override fun getApplicationContext(): Context = this
    }

    @After
    fun tearDown() {
        XRepo.resetForTest()
    }

    @Test
    fun configure_registersDescriptionAndSchemaWithDisabledDefault() {
        val config = LocalToolConfig()

        CreateCustomToolBuiltin().configure(config)

        assertTrue(config.description.contains("Create or update a custom tool"))
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
        val method = CreateCustomToolBuiltin::class.java.getDeclaredMethod(
            "parseArguments",
            String::class.java,
        )
        method.isAccessible = true

        val request = method.invoke(
            CreateCustomToolBuiltin(),
            """
            {
              "name": "battery_status",
              "description": "Read current battery status.",
              "command": "dumpsys battery"
            }
            """.trimIndent(),
        ) as CustomToolCreateRequest

        assertEquals("battery_status", request.name)
        assertEquals("Read current battery status.", request.description)
        assertEquals("dumpsys battery", request.command)
        assertFalse(request.enabled)
        assertFalse(request.overwrite)
    }

    @Test
    fun invoke_returnsInvalidArgumentsJsonForNonObjectArguments() = runTest {
        val result = CreateCustomToolBuiltin().invoke(
            BuiltinToolRequest(
                name = "create_custom_tool",
                argumentsJson = "[]",
            )
        )

        assertFalse(result.ok)
        assertEquals("INVALID_ARGUMENTS_JSON", result.code)
        assertTrue(result.fieldErrors.containsKey("argumentsJson"))
    }

    @Test
    fun invoke_writesCreatedToolThroughXRepo() = runTest {
        val store = FakeLocalSettingsStore(LocalSettings())
        XRepo.installStoreForTest(store)
        XRepo.init(context)

        val result = CreateCustomToolBuiltin().invoke(
            BuiltinToolRequest(
                name = "create_custom_tool",
                argumentsJson = """
                    {
                      "name": "battery_status",
                      "description": "Read current battery status.",
                      "command": "dumpsys battery",
                      "enabled": true,
                      "overwrite": false
                    }
                """.trimIndent(),
            )
        )

        assertTrue(result.ok)
        assertEquals(1, store.writeCount)
        assertEquals(
            listOf(CustomTool("battery_status", "Read current battery status.", "dumpsys battery", enabled = true)),
            XRepo.customTools.list(),
        )
    }

    private class FakeLocalSettingsStore(
        initialSettings: LocalSettings,
    ) : LocalSettingsStore {
        var settings: LocalSettings = initialSettings
            private set
        var writeCount: Int = 0
            private set

        override suspend fun read(context: Context): LocalSettings = settings

        override suspend fun write(context: Context, settings: LocalSettings) {
            this.settings = settings
            writeCount++
        }
    }
}
