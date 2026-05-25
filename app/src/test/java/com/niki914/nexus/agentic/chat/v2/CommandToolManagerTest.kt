package com.niki914.nexus.agentic.chat.v2

import com.niki914.nexus.agentic.chat.agentic.CustomToolCreateRequest
import com.niki914.nexus.agentic.chat.agentic.CustomToolManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CustomToolManagerTest {
    private val manager = CustomToolManager(reservedToolNames = { emptySet() })

    @Test
    fun validate_rejectsDangerousCommandsWithExecutablePathsOrAliases() {
        listOf(
            "/system/bin/reboot",
            "toybox reboot",
            "busybox rm -rf /data/local/tmp/cache",
            "/system/bin/dd if=/dev/zero of=/dev/block/by-name/userdata",
            "/system/bin/pm uninstall com.example.app",
        ).forEach { command ->
            assertUnsafe(command)
        }
    }

    @Test
    fun validate_rejectsDangerousRmFlagVariants() {
        listOf(
            "rm -fr /data/local/tmp/cache",
            "rm -r -f /data/local/tmp/cache",
            "rm --recursive --force /data/local/tmp/cache",
        ).forEach { command ->
            assertUnsafe(command)
        }
    }

    @Test
    fun validate_rejectsDangerousCommandsSplitByShellEscapesOrQuotes() {
        listOf(
            "r\\m -rf /data/local/tmp/cache",
            "\"r\"m -rf /data/local/tmp/cache",
            "'r'm -rf /data/local/tmp/cache",
        ).forEach { command ->
            assertUnsafe(command)
        }
    }

    @Test
    fun validate_rejectsCmdPackageUninstallAlias() {
        assertUnsafe("cmd package uninstall com.example.app")
    }

    @Test
    fun validate_rejectsShellWrappedDangerousPayloadsRecursively() {
        listOf(
            "sh -c 'rm -rf /data/local/tmp/cache'",
            "/system/bin/sh -c \"pm uninstall com.example.app\"",
            "eval 'cmd package uninstall com.example.app'",
            "sh -c \"eval 'dd if=/dev/zero of=/dev/block/by-name/userdata'\"",
        ).forEach { command ->
            assertUnsafe(command)
        }
    }

    @Test
    fun validate_allowsBenignCommand() {
        val result = manager.validate(
            request = request(command = "getprop ro.product.model"),
            existingNames = emptySet(),
            reservedNames = emptySet(),
        )

        assertNull(result)
    }

    @Test
    fun validate_allowsShellWrappedBenignPayload() {
        val result = manager.validate(
            request = request(command = "sh -c 'getprop ro.product.model'"),
            existingNames = emptySet(),
            reservedNames = emptySet(),
        )

        assertNull(result)
    }

    private fun assertUnsafe(command: String) {
        val result = manager.validate(
            request = request(command = command),
            existingNames = emptySet(),
            reservedNames = emptySet(),
        )

        assertEquals("UNSAFE_COMMAND", result?.code)
    }

    private fun request(command: String): CustomToolCreateRequest {
        return CustomToolCreateRequest(
            name = "sample_tool",
            description = "Sample custom tool.",
            command = command,
        )
    }
}
