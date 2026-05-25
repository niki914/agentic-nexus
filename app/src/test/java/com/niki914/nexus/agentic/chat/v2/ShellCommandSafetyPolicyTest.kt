package com.niki914.nexus.agentic.chat.v2

import com.niki914.nexus.agentic.chat.agentic.ShellCommandSafetyPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellCommandSafetyPolicyTest {
    private val policy = ShellCommandSafetyPolicy()

    @Test
    fun evaluate_rejectsDangerousCommands() {
        listOf(
            "/system/bin/reboot",
            "toybox reboot",
            "busybox rm -rf /data/local/tmp/cache",
            "sh -c 'pm uninstall com.example.app'",
            "eval 'dd if=/dev/zero of=/dev/block/by-name/userdata'",
        ).forEach { command ->
            assertFalse(policy.evaluate(command).allowed)
        }
    }

    @Test
    fun evaluate_allowsBenignCommands() {
        listOf(
            "getprop ro.product.model",
            "sh -c 'getprop ro.product.model'",
        ).forEach { command ->
            assertTrue(policy.evaluate(command).allowed)
        }
    }
}
