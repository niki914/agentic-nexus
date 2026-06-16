package com.niki914.nexus.agentic.chat

import com.niki914.libterm.OpenResult
import com.niki914.libterm.SshOpenOptions
import com.niki914.libterm.TerminalBytes
import com.niki914.libterm.TerminalIdentity
import com.niki914.libterm.runtime.CommandResult
import com.niki914.libterm.runtime.TermResult
import com.niki914.nexus.agentic.chat.agentic.buildin.BuiltinToolRequest
import com.niki914.nexus.agentic.chat.agentic.buildin.impl.SshTerminalBuiltin
import com.niki914.nexus.agentic.chat.agentic.shell.TerminalRuntimePort
import com.niki914.nexus.agentic.chat.agentic.shell.TerminalSessionPool
import com.niki914.nexus.agentic.chat.agentic.shell.TerminalSessionPort
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SshTerminalBuiltinTest {
    @After
    fun tearDown() {
        runTest {
            TerminalSessionPool.closeAll()
        }
    }

    @Test
    fun invoke_returnsRawJsonOnlyHintWithSshExample() = runTest {
        val result = SshTerminalBuiltin().invoke(
            BuiltinToolRequest(
                name = "ssh_terminal",
                argumentsJson = "{}",
            )
        )

        assertFalse(result.ok)
        assertEquals("RAW_JSON_ONLY", result.code)
        assertTrue(result.hint.contains("ssh_terminal"))
        assertTrue(result.hint.contains("open_and_exec"))
        assertTrue(result.hint.contains("host"))
        assertTrue(result.hint.contains("password"))
    }

    @Test
    fun configure_schemaContainsSshFields() {
        val tool = SshTerminalBuiltin()
        val config = com.niki914.s3ss10n.LocalToolConfig()

        tool.configure(config)

        val schema = Json.parseToJsonElement(config.rawInputSchemaJson!!).jsonObject
        val properties = schema["properties"]!!.jsonObject
        assertEquals(
            listOf("accept_any", "known_hosts_file"),
            properties["host_key_policy"]!!.jsonObject["enum"]!!.jsonArray.map { it.jsonPrimitive.content },
        )
        assertEquals("string", properties["password"]!!.jsonObject["type"]!!.jsonPrimitive.content)
        assertNull(properties["session"]!!.jsonObject["enum"])
    }

    @Test
    fun invokeRawJson_rejectsUnknownFields() = runTest {
        val json = invoke(
            """
            {
              "action":"open",
              "host":"example.com",
              "username":"alice",
              "password":"secret",
              "unexpected":true
            }
            """.trimIndent()
        )

        assertErrorCode("INVALID_REQUEST", json)
        assertTrue(json["error"]!!.jsonObject["message"]!!.jsonPrimitive.content.contains("Unknown ssh_terminal request field"))
    }

    @Test
    fun invokeRawJson_rejectsPrivateKeyFieldsForV1() = runTest {
        val json = invoke(
            """
            {
              "action":"open",
              "host":"example.com",
              "username":"alice",
              "private_key_pem":"-----BEGIN PRIVATE KEY-----",
              "password":"secret"
            }
            """.trimIndent()
        )

        assertErrorCode("INVALID_REQUEST", json)
        assertEquals(
            "Private key authentication is not supported yet.",
            json["error"]!!.jsonObject["message"]!!.jsonPrimitive.content,
        )
    }

    @Test
    fun invokeRawJson_openAndExecUsesSshSessionAndDoesNotEchoPassword() = runTest {
        installRuntimeSettingsGatewayForTest()
        val fakeRuntime = FakeTerminalRuntime(
            nextResult = commandResult(stdout = "ok\n"),
        )
        installFakeRuntime(fakeRuntime).use {
            installHandles("a3f9").use {
                val json = invoke(
                    """
                    {
                      "action":"open_and_exec",
                      "host":"example.com",
                      "port":2222,
                      "username":"alice",
                      "password":"secret",
                      "command":"pwd",
                      "host_key_policy":"known_hosts_file",
                      "known_hosts_path":"/data/local/tmp/known_hosts",
                      "strict_host_key_checking":false,
                      "connect_timeout_ms":1234,
                      "server_alive_interval_ms":5678
                    }
                    """.trimIndent()
                )

                assertEquals("a3f9", json["session"]!!.jsonPrimitive.content)
                assertEquals("ssh", json["identity"]!!.jsonPrimitive.content)
                assertEquals("0", json["exit_code"]!!.jsonPrimitive.content)
                assertEquals("ok\n", json["stdout"]!!.jsonPrimitive.content)
                assertEquals(listOf(TerminalIdentity.Ssh), fakeRuntime.openedIdentities)
                assertEquals("example.com", fakeRuntime.openedSshOptions.single()!!.host)
                assertEquals(2222, fakeRuntime.openedSshOptions.single()!!.port)
                assertEquals("alice", fakeRuntime.openedSshOptions.single()!!.username)
                assertTrue(json.toString().contains("secret").not())
            }
        }
    }

    private suspend fun invoke(argumentsJson: String) = Json.parseToJsonElement(
        SshTerminalBuiltin().invokeRawJson(
            BuiltinToolRequest(
                name = "ssh_terminal",
                argumentsJson = argumentsJson,
            )
        )
    ).jsonObject

    private fun assertErrorCode(expected: String, json: kotlinx.serialization.json.JsonObject) {
        assertEquals(expected, json["error"]!!.jsonObject["code"]!!.jsonPrimitive.content)
    }

    private fun installFakeRuntime(fakeRuntime: FakeTerminalRuntime): AutoCloseable {
        return TerminalSessionPool.installRuntimePortFactoryForTest { fakeRuntime }
    }

    private fun installHandles(vararg handles: String): AutoCloseable {
        val iterator = handles.iterator()
        return TerminalSessionPool.installHandleGeneratorForTest {
            check(iterator.hasNext()) { "No fake terminal handles left." }
            iterator.next()
        }
    }

    private class FakeTerminalRuntime(
        private val nextResult: CommandResult = commandResult(),
    ) : TerminalRuntimePort {
        val openedSessions = mutableListOf<FakeTerminalSession>()
        val openedIdentities = mutableListOf<TerminalIdentity>()
        val openedSshOptions = mutableListOf<SshOpenOptions?>()

        override suspend fun open(
            identity: TerminalIdentity,
            cwd: String?,
            sshOptions: SshOpenOptions?,
        ): OpenResult<TerminalSessionPort> {
            openedIdentities.add(identity)
            openedSshOptions.add(sshOptions)
            val session = FakeTerminalSession(
                id = "runtime-${openedSessions.size + 1}",
                nextResult = nextResult,
            )
            openedSessions.add(session)
            return OpenResult.Success(session)
        }

        override suspend fun close(sessionId: String) = Unit

        override suspend fun closeAll(): Int = openedSessions.size
    }

    private class FakeTerminalSession(
        override val id: String,
        private val nextResult: CommandResult,
    ) : TerminalSessionPort {
        override val stream = emptyFlow<com.niki914.libterm.runtime.TerminalTextChunk>()

        override suspend fun exec(command: String, timeoutMillis: Long): TermResult<CommandResult> {
            return TermResult.Success(nextResult)
        }

        override suspend fun close() = Unit
    }

    private companion object {
        fun commandResult(
            stdout: String = "",
            stderr: String = "",
            exitCode: Int? = 0,
            timedOut: Boolean = false,
        ): CommandResult {
            return CommandResult(
                command = "cmd",
                stdout = TerminalBytes.of(stdout.encodeToByteArray()),
                stderr = TerminalBytes.of(stderr.encodeToByteArray()),
                exitCode = exitCode,
                timedOut = timedOut,
            )
        }
    }
}
