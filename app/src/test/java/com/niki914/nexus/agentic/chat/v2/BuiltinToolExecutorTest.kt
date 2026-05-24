package com.niki914.nexus.agentic.chat.v2

import android.content.Context
import android.content.ContextWrapper
import com.niki914.nexus.agentic.chat.agentic.BuiltinTool
import com.niki914.nexus.agentic.chat.agentic.BuiltinToolExecutor
import com.niki914.nexus.agentic.chat.agentic.BuiltinToolRegistry
import com.niki914.nexus.agentic.chat.agentic.BuiltinToolRequest
import com.niki914.nexus.agentic.chat.agentic.BuiltinToolResult
import com.niki914.s3ss10n.LocalToolConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class BuiltinToolExecutorTest {
    private val context: Context = ContextWrapper(null)

    @Test
    fun execute_invokesBuiltinWithArgumentsJson() = runTest {
        val tool = RecordingBuiltinTool("create_command_tool")
        val executor = BuiltinToolExecutor(BuiltinToolRegistry(listOf(tool)))

        val resultJson = executor.execute(
            context = context,
            name = "create_command_tool",
            argumentsJson = """{"name":"battery_status"}""",
        )

        assertEquals("create_command_tool", tool.lastRequest?.name)
        assertEquals("""{"name":"battery_status"}""", tool.lastRequest?.argumentsJson)
        val json = Json.parseToJsonElement(resultJson).jsonObject
        assertEquals("OK", json["code"]!!.jsonPrimitive.content)
    }

    @Test
    fun execute_returnsLocalToolNotExecutableWhenBuiltinMissing() = runTest {
        val executor = BuiltinToolExecutor(BuiltinToolRegistry(emptyList()))

        val resultJson = executor.execute(context, "missing", "{}")

        val json = Json.parseToJsonElement(resultJson).jsonObject
        assertEquals("LOCAL_TOOL_NOT_EXECUTABLE", json["code"]!!.jsonPrimitive.content)
    }

    @Test
    fun execute_wrapsNonCancellationExceptionAsUnknownError() = runTest {
        val executor = BuiltinToolExecutor(
            BuiltinToolRegistry(listOf(ThrowingBuiltinTool("broken", IllegalStateException("boom"))))
        )

        val resultJson = executor.execute(context, "broken", "{}")

        val json = Json.parseToJsonElement(resultJson).jsonObject
        assertEquals("UNKNOWN_ERROR", json["code"]!!.jsonPrimitive.content)
        assertEquals("boom", json["message"]!!.jsonPrimitive.content)
    }

    @Test(expected = CancellationException::class)
    fun execute_rethrowsCancellationException() = runTest {
        val executor = BuiltinToolExecutor(
            BuiltinToolRegistry(listOf(ThrowingBuiltinTool("cancel", CancellationException("cancel"))))
        )

        executor.execute(context, "cancel", "{}")
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

    private class ThrowingBuiltinTool(
        override val name: String,
        private val throwable: Throwable,
    ) : BuiltinTool() {
        override fun configure(config: LocalToolConfig) = Unit

        override suspend fun invoke(request: BuiltinToolRequest): BuiltinToolResult {
            throw throwable
        }
    }
}
