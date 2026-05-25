package com.niki914.nexus.agentic.chat.v2

import android.content.Context
import android.content.ContextWrapper
import com.niki914.nexus.agentic.chat.LocalTool
import com.niki914.nexus.agentic.chat.ResolvedTools
import com.niki914.nexus.agentic.chat.agentic.BuiltinTool
import com.niki914.nexus.agentic.chat.agentic.BuiltinToolExecutor
import com.niki914.nexus.agentic.chat.agentic.BuiltinToolRegistry
import com.niki914.nexus.agentic.chat.agentic.BuiltinToolRequest
import com.niki914.nexus.agentic.chat.agentic.BuiltinToolResult
import com.niki914.nexus.agentic.chat.agentic.CustomToolExecutor
import com.niki914.nexus.agentic.chat.agentic.ToolCallDispatcher
import com.niki914.s3ss10n.LocalToolConfig
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class ToolCallDispatcherTest {
    private val context: Context = ContextWrapper(null)

    @Test
    fun executeLocalTool_dispatchesEnabledBuiltinBeforeCustomToolFallback() = runTest {
        val builtin = RecordingBuiltinTool("create_custom_tool")
        val dispatcher = ToolCallDispatcher(
            builtinToolExecutor = BuiltinToolExecutor(BuiltinToolRegistry(listOf(builtin))),
            customToolExecutor = CustomToolExecutor(),
            currentTools = {
                ResolvedTools(
                    builtinTools = listOf(
                        LocalTool.Builtin(
                            name = "create_custom_tool",
                            description = "Create custom tool",
                            tool = builtin,
                        )
                    ),
                    customTools = listOf(
                        LocalTool.Custom(
                            name = "create_custom_tool",
                            description = "Conflicting command",
                            enabled = true,
                            command = "date",
                        )
                    ),
                )
            },
        )

        val resultJson = dispatcher.executeLocalTool(
            context = context,
            name = "create_custom_tool",
            argumentsJson = """{"name":"battery_status"}""",
        )

        assertEquals("""{"name":"battery_status"}""", builtin.lastRequest?.argumentsJson)
        val json = Json.parseToJsonElement(resultJson).jsonObject
        assertEquals("OK", json["code"]!!.jsonPrimitive.content)
    }

    @Test
    fun executeLocalTool_usesResolvedBuiltinInstanceInsteadOfExecutorRegistryLookup() = runTest {
        val builtin = RecordingBuiltinTool("create_custom_tool")
        val dispatcher = ToolCallDispatcher(
            builtinToolExecutor = BuiltinToolExecutor(BuiltinToolRegistry(emptyList())),
            customToolExecutor = CustomToolExecutor(),
            currentTools = {
                ResolvedTools(
                    builtinTools = listOf(
                        LocalTool.Builtin(
                            name = "create_custom_tool",
                            description = "Create custom tool",
                            tool = builtin,
                        )
                    ),
                )
            },
        )

        val resultJson = dispatcher.executeLocalTool(
            context = context,
            name = "create_custom_tool",
            argumentsJson = """{"name":"battery_status"}""",
        )

        assertEquals("""{"name":"battery_status"}""", builtin.lastRequest?.argumentsJson)
        val json = Json.parseToJsonElement(resultJson).jsonObject
        assertEquals("OK", json["code"]!!.jsonPrimitive.content)
    }

    @Test
    fun executeLocalTool_returnsStructuredErrorForUnknownName() = runTest {
        val dispatcher = ToolCallDispatcher(
            builtinToolExecutor = BuiltinToolExecutor(BuiltinToolRegistry(emptyList())),
            customToolExecutor = CustomToolExecutor(),
            currentTools = { ResolvedTools() },
        )

        val resultJson = dispatcher.executeLocalTool(
            context = context,
            name = "missing",
            argumentsJson = "{}",
        )

        val json = Json.parseToJsonElement(resultJson).jsonObject
        assertEquals("LOCAL_TOOL_NOT_EXECUTABLE", json["code"]!!.jsonPrimitive.content)
    }

    @Test
    fun executeLocalTool_fallsBackToCustomToolWhenBuiltinIsNotEnabled() = runTest {
        val dispatcher = ToolCallDispatcher(
            builtinToolExecutor = BuiltinToolExecutor(BuiltinToolRegistry(emptyList())),
            customToolExecutor = CustomToolExecutor(timeoutMs = 1),
            currentTools = {
                ResolvedTools(
                    customTools = listOf(
                        LocalTool.Custom(
                            name = "device_model",
                            description = "Read device model",
                            enabled = true,
                            command = "getprop ro.product.model",
                        )
                    )
                )
            },
        )

        val resultJson = dispatcher.executeLocalTool(
            context = context,
            name = "device_model",
            argumentsJson = "{}",
        )

        val json = Json.parseToJsonElement(resultJson).jsonObject
        assertEquals("getprop ro.product.model", json["command"]!!.jsonPrimitive.content)
    }

    private class RecordingBuiltinTool(
        override val name: String,
    ) : BuiltinTool() {
        var lastRequest: BuiltinToolRequest? = null

        override fun configure(config: LocalToolConfig) = Unit

        override suspend fun invoke(request: BuiltinToolRequest): BuiltinToolResult {
            lastRequest = request
            return BuiltinToolResult.success(message = "ok")
        }
    }
}
