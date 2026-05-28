package com.niki914.nexus.agentic.chat

import com.niki914.nexus.agentic.chat.agentic.buildin.BuiltinToolRequest
import com.niki914.nexus.agentic.chat.agentic.buildin.impl.RunCommandBuildin_WIP_SAFE
import com.niki914.nexus.agentic.chat.agentic.shell.ShellCommandRunner
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class RunCommandBuiltinTest {
    @Test
    fun invokeRawJson_rejectsBlockedCommand() = runTest {
        val tool = RunCommandBuildin_WIP_SAFE()
        val json = Json.parseToJsonElement(
            tool.invokeRawJson(
                BuiltinToolRequest(
                    name = tool.name,
                    argumentsJson = """{"command":"rm -rf /data/local/tmp/cache"}""",
                )
            )
        ).jsonObject

        assertEquals("-2", json["exit_code"]!!.jsonPrimitive.content)
        assertEquals("Command blocked by safety policy.", json["stderr"]!!.jsonPrimitive.content)
    }

    @Test
    fun invokeRawJson_supportsWorkdirAndMergeStderr() = runTest {
        val tool = RunCommandBuildin_WIP_SAFE(
            shellCommandRunner = ShellCommandRunner("/bin/sh"),
        )
        val json = Json.parseToJsonElement(
            tool.invokeRawJson(
                BuiltinToolRequest(
                    name = tool.name,
                    argumentsJson = """{"command":"pwd; echo err >&2","workdir":"/tmp","merge_stderr":true}""",
                )
            )
        ).jsonObject

        assertEquals("0", json["exit_code"]!!.jsonPrimitive.content)
        assertEquals(true, json["stdout"]!!.jsonPrimitive.content.contains("/tmp"))
        assertEquals(true, json["stdout"]!!.jsonPrimitive.content.contains("err"))
    }

    @Test
    fun invokeRawJson_rejectsInvalidTimeoutOverride() = runTest {
        val tool = RunCommandBuildin_WIP_SAFE()
        val json = Json.parseToJsonElement(
            tool.invokeRawJson(
                BuiltinToolRequest(
                    name = tool.name,
                    argumentsJson = """{"command":"pwd","timeout_ms":0}""",
                )
            )
        ).jsonObject

        assertEquals("-1", json["exit_code"]!!.jsonPrimitive.content)
        assertEquals(
            "Field 'timeout_ms' must be a positive integer.",
            json["stderr"]!!.jsonPrimitive.content
        )
    }
}
