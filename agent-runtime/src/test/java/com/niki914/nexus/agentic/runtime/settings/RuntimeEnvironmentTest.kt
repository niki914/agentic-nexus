package com.niki914.nexus.agentic.runtime.settings

import com.niki914.nexus.agentic.runtime.settings.model.RuntimeBuiltinToolSetting
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeCustomTool
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeCustomToolValidation
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeLlmConfig
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeMcpServer
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeMcpTool
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Test

class RuntimeEnvironmentTest {
    @After
    fun tearDown() {
        RuntimeEnvironment.clearForTest()
    }

    @Test
    fun awaitSettingsGateway_waitsForDelayedInstall() = runTest {
        val gateway = FakeRuntimeSettingsGateway()
        val awaiting = async { RuntimeEnvironment.awaitSettingsGateway() }

        RuntimeEnvironment.install(gateway)

        assertSame(gateway, awaiting.await())
    }
}

private class FakeRuntimeSettingsGateway : RuntimeSettingsGateway {
    override suspend fun readLlmConfig(): RuntimeLlmConfig = RuntimeLlmConfig()

    override suspend fun listMcpServers(): List<RuntimeMcpServer> = emptyList()

    override suspend fun listCachedTools(server: RuntimeMcpServer): List<RuntimeMcpTool> = emptyList()

    override suspend fun saveDiscoveredTools(
        url: String,
        headers: Map<String, String>,
        tools: List<RuntimeMcpTool>,
    ) = Unit

    override suspend fun clearMcpCacheByServerNames(names: Set<String>) = Unit

    override suspend fun fingerprintMcpServers(): String = ""

    override suspend fun listCustomTools(): List<RuntimeCustomTool> = emptyList()

    override suspend fun saveCustomTool(
        tool: RuntimeCustomTool,
        overwrite: Boolean,
    ): RuntimeCustomToolValidation? = null

    override suspend fun replaceAllCustomTools(
        tools: List<RuntimeCustomTool>,
    ): RuntimeCustomToolValidation? = null

    override suspend fun deleteCustomTool(name: String) = Unit

    override suspend fun setCustomToolEnabled(name: String, enabled: Boolean) = Unit

    override suspend fun listBuiltinToolSettings(): List<RuntimeBuiltinToolSetting> = emptyList()

    override suspend fun setBuiltinToolEnabled(
        name: String,
        enabled: Boolean,
    ): RuntimeCustomToolValidation? = null
}
