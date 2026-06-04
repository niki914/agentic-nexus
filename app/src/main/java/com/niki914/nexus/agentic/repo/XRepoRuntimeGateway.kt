package com.niki914.nexus.agentic.repo

import com.niki914.nexus.agentic.runtime.settings.RuntimeSettingsGateway
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeBuiltinToolSetting
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeCustomTool
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeCustomToolValidation
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeLlmConfig
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeMcpServer
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeMcpTool

class XRepoRuntimeGateway(
    private val repo: XRepo = XRepo,
) : RuntimeSettingsGateway {
    override suspend fun readLlmConfig(): RuntimeLlmConfig = repo.llm()

    override suspend fun listMcpServers(): List<RuntimeMcpServer> = repo.mcp.list()

    override suspend fun listCachedTools(server: RuntimeMcpServer): List<RuntimeMcpTool> {
        return repo.mcp.cachedTools(server)
    }

    override suspend fun saveDiscoveredTools(
        url: String,
        headers: Map<String, String>,
        tools: List<RuntimeMcpTool>,
    ) {
        repo.mcp.saveDiscoveredTools(url, headers, tools)
    }

    override suspend fun clearMcpCacheByServerNames(names: Set<String>) {
        repo.mcp.clearCacheByServerNames(names)
    }

    override suspend fun fingerprintMcpServers(): String = repo.mcp.fingerprint()

    override suspend fun addMemory(value: String) {
        repo.memory.add(value)
    }

    override suspend fun listCustomTools(): List<RuntimeCustomTool> = repo.customTools.list()

    override suspend fun saveCustomTool(
        tool: RuntimeCustomTool,
        overwrite: Boolean,
    ): RuntimeCustomToolValidation? {
        return repo.customTools.save(tool, overwrite)
    }

    override suspend fun replaceAllCustomTools(
        tools: List<RuntimeCustomTool>,
    ): RuntimeCustomToolValidation? {
        return repo.customTools.replaceAll(tools)
    }

    override suspend fun deleteCustomTool(name: String) {
        repo.customTools.delete(name)
    }

    override suspend fun setCustomToolEnabled(name: String, enabled: Boolean) {
        repo.customTools.setEnabled(name, enabled)
    }

    override suspend fun listBuiltinToolSettings(): List<RuntimeBuiltinToolSetting> {
        return repo.builtinTools.list()
    }

    override suspend fun setBuiltinToolEnabled(
        name: String,
        enabled: Boolean,
    ): RuntimeCustomToolValidation? {
        return repo.builtinTools.setEnabled(name, enabled)
    }
}
