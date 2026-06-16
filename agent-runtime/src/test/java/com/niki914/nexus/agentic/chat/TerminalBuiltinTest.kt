package com.niki914.nexus.agentic.chat

import com.niki914.nexus.agentic.chat.agentic.buildin.BuiltinToolRequest
import com.niki914.nexus.agentic.chat.agentic.buildin.impl.TerminalBuiltin
import com.niki914.nexus.agentic.runtime.settings.RuntimeEnvironment
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeExecutionRule as ExecutionRule
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeExecutionRuleEnabledMode as ExecutionRuleEnabledMode

class TerminalBuiltinTest {
    @After
    fun tearDown() {
        RuntimeEnvironment.clearForTest()
    }

    @Test
    fun invokeRawJson_rejectsInvalidJson() = runTest {
        val json = invoke("""{"action":""")

        assertErrorCode("INVALID_REQUEST", json)
        assertEquals("argumentsJson is not valid JSON.", json["error"]!!.jsonObject["message"]!!.jsonPrimitive.content)
    }

    @Test
    fun invokeRawJson_rejectsUnknownAction() = runTest {
        val json = invoke("""{"action":"unknown"}""")

        assertErrorCode("INVALID_REQUEST", json)
        assertTrue(json["error"]!!.jsonObject["message"]!!.jsonPrimitive.content.contains("open_and_exec"))
    }

    @Test
    fun invokeRawJson_rejectsBlankCommand() = runTest {
        val json = invoke("""{"action":"exec","session":"user","command":"   "}""")

        assertErrorCode("INVALID_REQUEST", json)
        assertEquals("Field 'command' must not be blank.", json["error"]!!.jsonObject["message"]!!.jsonPrimitive.content)
    }

    @Test
    fun invokeRawJson_rejectsBlockedOpenAndExecBeforeOpeningSession() = runTest {
        installRuntimeSettingsGatewayForTest(
            FakeRuntimeSettingsGateway(executionRules = dangerousRules())
        )

        val json = invoke(
            """{"action":"open_and_exec","identity":"user","command":"rm -rf /data/local/tmp/cache"}"""
        )

        assertErrorCode("COMMAND_BLOCKED", json)
        assertEquals("0", json["elapsed_seconds"]!!.jsonPrimitive.content)
        assertEquals(
            "dangerous-command",
            json["error"]!!.jsonObject["matched_rule_id"]!!.jsonPrimitive.content,
        )
    }

    @Test
    fun invokeRawJson_execReturnsSessionNotFoundWithoutOpening() = runTest {
        installRuntimeSettingsGatewayForTest()

        val json = invoke("""{"action":"exec","session":"user","command":"pwd"}""")

        assertErrorCode("SESSION_NOT_FOUND", json)
    }

    @Test
    fun invokeRawJson_asyncExecReturnsSessionNotFoundWithoutOpening() = runTest {
        installRuntimeSettingsGatewayForTest()

        val json = invoke("""{"action":"exec","session":"user","command":"sleep 10","is_async":true}""")

        assertErrorCode("SESSION_NOT_FOUND", json)
    }

    @Test
    fun invokeRawJson_readAsyncResultReturnsSessionNotFoundWithoutOpening() = runTest {
        val json = invoke("""{"action":"read_async_result","session":"user","async_id":"a1"}""")

        assertErrorCode("SESSION_NOT_FOUND", json)
    }

    @Test
    fun invokeRawJson_closeIsIdempotentForMissingSession() = runTest {
        val json = invoke("""{"action":"close","session":"user"}""")

        assertTrue(json["closed"]!!.jsonPrimitive.content.toBoolean())
        assertFalse(json.containsKey("error"))
    }

    @Test
    fun invokeRawJson_rejectsInvalidTimeoutOverride() = runTest {
        val json = invoke("""{"action":"exec","session":"user","command":"pwd","timeout_ms":0}""")

        assertErrorCode("INVALID_REQUEST", json)
        assertEquals(
            "Field 'timeout_ms' must be greater than 0.",
            json["error"]!!.jsonObject["message"]!!.jsonPrimitive.content,
        )
    }

    private suspend fun invoke(argumentsJson: String) = Json.parseToJsonElement(
        TerminalBuiltin().invokeRawJson(
            BuiltinToolRequest(
                name = "terminal",
                argumentsJson = argumentsJson,
            )
        )
    ).jsonObject

    private fun assertErrorCode(expected: String, json: kotlinx.serialization.json.JsonObject) {
        assertEquals(expected, json["error"]!!.jsonObject["code"]!!.jsonPrimitive.content)
    }

    private fun dangerousRules(): List<ExecutionRule> {
        return listOf(
            ExecutionRule(
                id = "dangerous-command",
                name = "危险命令",
                enabledMode = ExecutionRuleEnabledMode.ALWAYS,
                patterns = listOf("\\brm\\s+-rf\\b"),
            )
        )
    }
}
