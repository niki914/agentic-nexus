package com.niki914.nexus.agentic.chat

import com.niki914.nexus.agentic.chat.agentic.custom.CustomToolConfig
import com.niki914.nexus.agentic.chat.agentic.custom.CustomToolCreateRequest
import com.niki914.nexus.agentic.chat.agentic.custom.CustomToolManager
import com.niki914.nexus.agentic.runtime.settings.RuntimeEnvironment
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeCustomTool as CustomTool
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomToolManagerTest {
    private val manager = CustomToolManager(reservedToolNames = { emptySet() })

    @After
    fun tearDown() {
        RuntimeEnvironment.clearForTest()
    }

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

    @Test
    fun createOrUpdate_writesThroughRuntimeSettingsGateway() = runTest {
        val store = installRuntimeSettingsGatewayForTest()

        val result = manager.createOrUpdate(
            request = request(command = "getprop ro.product.model").copy(enabled = true),
        )

        assertTrue(result.ok)
        assertEquals(1, store.writeCount)
        assertEquals(
            listOf(
                CustomTool(
                    name = "sample_tool",
                    description = "Sample custom tool.",
                    command = "getprop ro.product.model",
                    enabled = true,
                )
            ),
            store.customTools,
        )
    }

    @Test
    fun createOrUpdate_rejectsDuplicateNameWithoutWriting() = runTest {
        val store = installRuntimeSettingsGatewayForTest()
        assertTrue(
            manager.createOrUpdate(
                request = request(command = "getprop ro.product.model"),
            ).ok
        )

        val result = manager.createOrUpdate(
            request = request(command = "getprop ro.build.version.release"),
        )

        assertFalse(result.ok)
        assertEquals("NAME_CONFLICT", result.code)
        assertEquals(1, store.writeCount)
        assertEquals("getprop ro.product.model", store.customTools.single().command)
    }

    @Test
    fun createOrUpdate_rejectsUnsafeCommandWithoutWriting() = runTest {
        val store = installRuntimeSettingsGatewayForTest()

        val result = manager.createOrUpdate(
            request = request(command = "rm -rf /data/local/tmp/cache"),
        )

        assertFalse(result.ok)
        assertEquals("UNSAFE_COMMAND", result.code)
        assertEquals(0, store.writeCount)
        assertEquals(emptyList<CustomTool>(), store.customTools)
    }

    @Test
    fun saveAll_replacesRuntimeCustomTools() = runTest {
        val store = installRuntimeSettingsGatewayForTest()
        assertTrue(
            manager.createOrUpdate(
                request = request(command = "getprop ro.product.model").copy(name = "old_tool"),
            ).ok
        )

        val result = manager.saveAll(
            items = listOf(
                CustomToolConfig(
                    name = "new_tool",
                    description = "New custom tool.",
                    enabled = true,
                    command = "getprop ro.build.version.release",
                )
            ),
        )

        assertTrue(result.ok)
        assertEquals(
            listOf(
                CustomTool(
                    name = "new_tool",
                    description = "New custom tool.",
                    command = "getprop ro.build.version.release",
                    enabled = true,
                )
            ),
            store.customTools,
        )
    }

    @Test
    fun saveAll_writeFailureDoesNotPartiallyReplaceExistingTools() = runTest {
        val existing = CustomTool(
            name = "old_tool",
            description = "Old custom tool.",
            command = "getprop ro.product.model",
            enabled = true,
        )
        val store = installRuntimeSettingsGatewayForTest(
            FakeRuntimeSettingsGateway(customTools = listOf(existing))
        )
        store.failOnWriteNumber = store.writeCount + 1

        val result = manager.saveAll(
            items = listOf(
                CustomToolConfig(
                    name = "new_tool",
                    description = "New custom tool.",
                    enabled = true,
                    command = "getprop ro.build.version.release",
                )
            ),
        )

        assertFalse(result.ok)
        assertEquals(listOf(existing), store.customTools)
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
