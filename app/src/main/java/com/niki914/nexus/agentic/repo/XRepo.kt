package com.niki914.nexus.agentic.repo

import android.content.Context
import com.niki914.nexus.agentic.chat.agentic.buildin.BuiltinToolRegistry
import com.niki914.nexus.agentic.chat.agentic.shell.ShellCommandSafetyPolicy
import com.niki914.nexus.agentic.mod.LocalSettings
import com.niki914.nexus.h.util.ContextProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeBuiltinToolSetting as BuiltinToolSetting
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeCustomTool as CustomTool
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeCustomToolValidation as CustomToolValidation
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeExecutionRule as ExecutionRule
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeExecutionRuleEnabledMode as ExecutionRuleEnabledMode
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeLlmConfig as LlmConfig
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeMcpServer as McpServer
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeMcpTool as McpTool
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeTakeoverRule as TakeoverRule
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeTakeoverRuleValidation as TakeoverRuleValidation
import com.niki914.nexus.agentic.runtime.settings.model.TAKEOVER_FIELD_NAME
import com.niki914.nexus.agentic.runtime.settings.model.TAKEOVER_FIELD_PATTERNS

object XRepo {
    val mcp: McpApi = McpApi(this)
    val customTools: CustomToolApi = CustomToolApi(this)
    val builtinTools: BuiltinToolApi = BuiltinToolApi(this)
    val memory: MemoryApi = MemoryApi(this)
    val web: WebSettingsApi = WebSettingsApi(this)
    val executionRules: ExecutionRulesApi = ExecutionRulesApi(this)
    val takeoverRules: TakeoverRulesApi = TakeoverRulesApi(this)

    private val writeMutex = Mutex()
    private var appContext: Context? = null
    private var store: LocalSettingsStore = XServiceLocalSettingsStore

    fun init(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext ?: context
        }
    }

    internal fun installStoreForTest(store: LocalSettingsStore) {
        this.store = store
    }

    internal fun resetForTest() {
        appContext = null
        store = XServiceLocalSettingsStore
    }

    internal suspend fun context(): Context {
        appContext?.let { return it }
        val context = ContextProvider.await()
        init(context)
        return appContext ?: context
    }

    internal suspend fun readLocal(): LocalSettings {
        return store.read(context())
    }

    internal suspend fun updateLocal(transform: (LocalSettings) -> LocalSettings): LocalSettings {
        return writeMutex.withLock {
            val context = context()
            val latest = store.read(context)
            val updated = transform(latest)
            store.write(context, updated)
            updated
        }
    }

    internal suspend fun updateLocalOrNull(transform: (LocalSettings) -> LocalSettings?): LocalSettings? {
        return writeMutex.withLock {
            val context = context()
            val latest = store.read(context)
            val updated = transform(latest) ?: return@withLock null
            store.write(context, updated)
            updated
        }
    }

    suspend fun tryPutDefaultSettings(): Boolean {
        return writeMutex.withLock {
            val context = context()
            val latest = store.read(context)
            if (latest.onboardingCompleted) {
                return@withLock false
            }
            store.write(context, LocalSettingsDefaults.applyTo(latest))
            true
        }
    }

    suspend fun onboardingCompleted(): Boolean {
        return readLocal().onboardingCompleted
    }

    suspend fun setOnboardingCompleted(value: Boolean) {
        updateLocal { settings ->
            LocalSettingsCodec.withBoolean(settings, ONBOARDING_COMPLETED_KEY, value)
        }
    }

    suspend fun llm(): LlmConfig {
        return LocalSettingsCodec.parseLlm(readLocal())
    }

    suspend fun saveLlmAccess(
        provider: String,
        endpoint: String,
        model: String,
        apiKey: String,
    ) {
        updateLocal { settings ->
            LocalSettingsCodec.withLlmAccess(
                settings = settings,
                provider = provider,
                endpoint = endpoint,
                model = model,
                apiKey = apiKey,
            )
        }
    }

    suspend fun saveLlm(config: LlmConfig) {
        updateLocal { settings ->
            LocalSettingsCodec.withLlm(settings, config)
        }
    }

    private const val ONBOARDING_COMPLETED_KEY = "onboarding_completed"
}

class MemoryApi internal constructor(
    private val repo: XRepo,
) {
    suspend fun list(): List<String> {
        return LocalSettingsCodec.parseMemories(repo.readLocal())
    }

    suspend fun replaceAll(memories: List<String>) {
        repo.updateLocal { settings ->
            LocalSettingsCodec.withMemories(settings, normalizeMemories(memories))
        }
    }

    suspend fun add(value: String) {
        val normalizedValue = value.trim()
        if (normalizedValue.isBlank()) {
            return
        }
        repo.updateLocal { settings ->
            val updated = LocalSettingsCodec.parseMemories(settings) + normalizedValue
            LocalSettingsCodec.withMemories(settings, updated)
        }
    }

    suspend fun update(index: Int, value: String) {
        val normalizedValue = value.trim()
        repo.updateLocalOrNull { settings ->
            val currentMemories = LocalSettingsCodec.parseMemories(settings)
            if (index !in currentMemories.indices) {
                return@updateLocalOrNull null
            }
            val updated = if (normalizedValue.isBlank()) {
                currentMemories.filterIndexed { itemIndex, _ -> itemIndex != index }
            } else {
                currentMemories.mapIndexed { itemIndex, item ->
                    if (itemIndex == index) normalizedValue else item
                }
            }
            LocalSettingsCodec.withMemories(settings, updated)
        }
    }

    suspend fun delete(index: Int) {
        repo.updateLocalOrNull { settings ->
            val currentMemories = LocalSettingsCodec.parseMemories(settings)
            if (index !in currentMemories.indices) {
                return@updateLocalOrNull null
            }
            LocalSettingsCodec.withMemories(
                settings = settings,
                memories = currentMemories.filterIndexed { itemIndex, _ -> itemIndex != index },
            )
        }
    }

    private fun normalizeMemories(memories: List<String>): List<String> {
        return memories.map(String::trim).filter(String::isNotBlank)
    }
}

class ExecutionRulesApi internal constructor(
    private val repo: XRepo,
) {
    suspend fun list(): List<ExecutionRule> {
        return listFromSettings(repo.readLocal())
    }

    suspend fun get(id: String): ExecutionRule? {
        return list().firstOrNull { it.id == id }
    }

    suspend fun save(rule: ExecutionRule) {
        repo.updateLocal { settings ->
            val rules = LocalSettingsCodec.parseExecutionRules(settings)
            val updated = if (rules.any { it.id == rule.id }) {
                rules.map { if (it.id == rule.id) rule else it }
            } else {
                rules + rule
            }
            LocalSettingsCodec.withExecutionRules(settings, updated)
        }
    }

    suspend fun replace(previousId: String?, rule: ExecutionRule) {
        repo.updateLocal { settings ->
            val rules = LocalSettingsCodec.parseExecutionRules(settings)
            val withoutPrevious = if (previousId != null && previousId != rule.id) {
                rules.filterNot { it.id == previousId }
            } else {
                rules
            }
            val updated = if (withoutPrevious.any { it.id == rule.id }) {
                withoutPrevious.map { if (it.id == rule.id) rule else it }
            } else {
                withoutPrevious + rule
            }
            LocalSettingsCodec.withExecutionRules(settings, updated)
        }
    }

    suspend fun delete(id: String) {
        repo.updateLocal { settings ->
            LocalSettingsCodec.withExecutionRules(
                settings = settings,
                rules = LocalSettingsCodec.parseExecutionRules(settings).filterNot { it.id == id },
            )
        }
    }

    suspend fun setEnabledMode(id: String, enabledMode: ExecutionRuleEnabledMode) {
        repo.updateLocal { settings ->
            LocalSettingsCodec.withExecutionRules(
                settings = settings,
                rules = LocalSettingsCodec.parseExecutionRules(settings).map { rule ->
                    if (rule.id == id) rule.copy(enabledMode = enabledMode) else rule
                },
            )
        }
    }

    private fun listFromSettings(settings: LocalSettings): List<ExecutionRule> {
        if (settings.shellSafetyPolicies == null) {
            return LocalSettingsDefaults.defaultExecutionRules
        }
        return LocalSettingsCodec.parseExecutionRules(settings)
    }
}

class TakeoverRulesApi internal constructor(
    private val repo: XRepo,
) {
    suspend fun list(): List<TakeoverRule> {
        return LocalSettingsCodec.parseTakeoverRules(repo.readLocal())
    }

    suspend fun get(id: String): TakeoverRule? {
        return list().firstOrNull { it.id == id }
    }

    suspend fun replace(previousId: String?, rule: TakeoverRule) {
        repo.updateLocal { settings ->
            val rules = LocalSettingsCodec.parseTakeoverRules(settings)
            val withoutPrevious = if (previousId != null && previousId != rule.id) {
                rules.filterNot { it.id == previousId }
            } else {
                rules
            }
            val updated = if (withoutPrevious.any { it.id == rule.id }) {
                withoutPrevious.map { if (it.id == rule.id) rule else it }
            } else {
                withoutPrevious + rule
            }
            LocalSettingsCodec.withTakeoverRules(settings, updated)
        }
    }

    suspend fun delete(id: String) {
        repo.updateLocal { settings ->
            LocalSettingsCodec.withTakeoverRules(
                settings = settings,
                rules = LocalSettingsCodec.parseTakeoverRules(settings).filterNot { it.id == id },
            )
        }
    }

    suspend fun setEnabled(id: String, enabled: Boolean) {
        repo.updateLocal { settings ->
            LocalSettingsCodec.withTakeoverRules(
                settings = settings,
                rules = LocalSettingsCodec.parseTakeoverRules(settings).map { rule ->
                    if (rule.id == id) rule.copy(enabled = enabled) else rule
                },
            )
        }
    }

    fun validate(rule: TakeoverRule): List<TakeoverRuleValidation> {
        val errors = mutableListOf<TakeoverRuleValidation>()
        if (rule.name.trim().isBlank()) {
            errors += TakeoverRuleValidation(
                field = TAKEOVER_FIELD_NAME,
                message = "Required field 'name' is missing.",
            )
        }
        if (rule.patterns.map(String::trim).filter(String::isNotBlank).isEmpty()) {
            errors += TakeoverRuleValidation(
                field = TAKEOVER_FIELD_PATTERNS,
                message = "At least one takeover pattern is required.",
            )
        }
        return errors
    }
}

class McpApi internal constructor(
    private val repo: XRepo,
) {
    suspend fun list(): List<McpServer> {
        return LocalSettingsCodec.parseMcpServers(repo.readLocal())
    }

    suspend fun get(name: String): McpServer? {
        return list().firstOrNull { it.name == name }
    }

    suspend fun save(server: McpServer) {
        repo.updateLocal { settings ->
            val servers = LocalSettingsCodec.parseMcpServers(settings)
            val updated = if (servers.any { it.name == server.name }) {
                servers.map { if (it.name == server.name) server else it }
            } else {
                servers + server
            }
            LocalSettingsCodec.withMcpServers(settings, updated)
        }
    }

    suspend fun replace(previousName: String?, server: McpServer) {
        repo.updateLocal { settings ->
            val servers = LocalSettingsCodec.parseMcpServers(settings)
            val withoutPrevious = if (previousName != null && previousName != server.name) {
                servers.filterNot { it.name == previousName }
            } else {
                servers
            }
            val updated = if (withoutPrevious.any { it.name == server.name }) {
                withoutPrevious.map { if (it.name == server.name) server else it }
            } else {
                withoutPrevious + server
            }
            LocalSettingsCodec.withMcpServers(settings, updated)
        }
    }

    suspend fun delete(name: String) {
        repo.updateLocal { settings ->
            val servers = LocalSettingsCodec.parseMcpServers(settings)
            LocalSettingsCodec.withMcpServers(
                settings = settings,
                servers = servers.filterNot { it.name == name },
            )
        }
    }

    suspend fun setEnabled(name: String, enabled: Boolean) {
        repo.updateLocal { settings ->
            val servers = LocalSettingsCodec.parseMcpServers(settings)
            LocalSettingsCodec.withMcpServers(
                settings = settings,
                servers = servers.map { server ->
                    if (server.name == name) server.copy(enabled = enabled) else server
                },
            )
        }
    }

    suspend fun cachedTools(server: McpServer): List<McpTool> {
        return LocalSettingsCodec.parseMcpCache(repo.readLocal(), server)
    }

    suspend fun saveDiscoveredTools(
        url: String,
        headers: Map<String, String>,
        tools: List<McpTool>,
    ) {
        repo.updateLocal { settings ->
            LocalSettingsCodec.withMcpCache(
                settings = settings,
                url = url,
                headers = headers,
                tools = tools,
            )
        }
    }

    suspend fun clearCache(server: McpServer) {
        repo.updateLocal { settings ->
            LocalSettingsCodec.withoutMcpCache(settings, listOf(server))
        }
    }

    suspend fun clearCacheByServerNames(names: Set<String>) {
        repo.updateLocal { settings ->
            val targets = LocalSettingsCodec.parseMcpServers(settings)
                .filter { it.name in names }
            LocalSettingsCodec.withoutMcpCache(settings, targets)
        }
    }

    suspend fun fingerprint(): String {
        return list()
            .sortedBy { it.name }
            .joinToString(separator = "\n") { server ->
                val headers = server.headers
                    .mapKeys { (key, _) -> key.lowercase() }
                    .toSortedMap()
                    .entries
                    .joinToString(separator = "&") { (key, value) -> "$key=$value" }
                "${server.name}|${server.url}|${server.enabled}|$headers"
            }
    }
}

class CustomToolApi internal constructor(
    private val repo: XRepo,
    private val safetyPolicy: ShellCommandSafetyPolicy = ShellCommandSafetyPolicy(
        listExecutionRules = { repo.executionRules.list() },
    ),
    private val builtinToolRegistry: BuiltinToolRegistry = BuiltinToolRegistry.default(),
) {
    suspend fun list(): List<CustomTool> {
        return LocalSettingsCodec.parseCustomTools(repo.readLocal())
    }

    suspend fun get(name: String): CustomTool? {
        return list().firstOrNull { it.name == name }
    }

    suspend fun save(tool: CustomTool, overwrite: Boolean = true): CustomToolValidation? {
        validate(tool, overwrite)?.let { return it }
        val normalized = tool.normalized()
        repo.updateLocal { settings ->
            val tools = LocalSettingsCodec.parseCustomTools(settings)
            val updated = if (tools.any { it.name == normalized.name }) {
                tools.map { if (it.name == normalized.name) normalized else it }
            } else {
                tools + normalized
            }
            LocalSettingsCodec.withCustomTools(settings, updated)
        }
        return null
    }

    suspend fun replace(previousName: String?, tool: CustomTool): CustomToolValidation? {
        validate(tool, overwrite = true)?.let { return it }
        val normalized = tool.normalized()
        val existingTools = list()
        val withoutPrevious = if (previousName != null) {
            existingTools.filterNot { it.name == previousName }
        } else {
            existingTools
        }
        if (withoutPrevious.any { it.name == normalized.name }) {
            return CustomToolValidation("name", "Already exists in custom_tools.")
        }
        repo.updateLocal { settings ->
            val tools = LocalSettingsCodec.parseCustomTools(settings)
            val withoutPrevious = previousName
                ?.let { name -> tools.filterNot { it.name == name } }
                ?: tools
            val updated = withoutPrevious + normalized
            LocalSettingsCodec.withCustomTools(settings, updated)
        }
        return null
    }

    suspend fun replaceAll(tools: List<CustomTool>): CustomToolValidation? {
        val normalizedTools = tools.map { it.normalized() }
        val duplicateName = normalizedTools
            .groupingBy { it.name }
            .eachCount()
            .entries
            .firstOrNull { it.value > 1 }
            ?.key
        if (duplicateName != null) {
            return CustomToolValidation("name", "Duplicate name in custom_tools.")
        }
        normalizedTools.forEach { tool ->
            validate(tool, overwrite = true)?.let { return it }
        }
        repo.updateLocal { settings ->
            LocalSettingsCodec.withCustomTools(settings, normalizedTools)
        }
        return null
    }

    suspend fun delete(name: String) {
        repo.updateLocal { settings ->
            LocalSettingsCodec.withCustomTools(
                settings = settings,
                tools = LocalSettingsCodec.parseCustomTools(settings).filterNot { it.name == name },
            )
        }
    }

    suspend fun setEnabled(name: String, enabled: Boolean) {
        repo.updateLocal { settings ->
            LocalSettingsCodec.withCustomTools(
                settings = settings,
                tools = LocalSettingsCodec.parseCustomTools(settings).map { tool ->
                    if (tool.name == name) tool.copy(enabled = enabled) else tool
                },
            )
        }
    }

    suspend fun validate(tool: CustomTool, overwrite: Boolean = true): CustomToolValidation? {
        val normalized = tool.normalized()
        if (normalized.name.isBlank()) {
            return CustomToolValidation("name", "Required field 'name' is missing.")
        }
        if (!NAME_PATTERN.matches(normalized.name)) {
            return CustomToolValidation(
                field = "name",
                message = "Name must start with a letter or underscore and contain only letters, digits, or underscores.",
            )
        }
        if (normalized.description.isBlank()) {
            return CustomToolValidation("description", "Required field 'description' is missing.")
        }
        if (normalized.command.isBlank()) {
            return CustomToolValidation("command", "Required field 'command' is missing.")
        }
        if (normalized.name in builtinToolRegistry.all().map { it.name }.toSet()) {
            return CustomToolValidation("name", "Reserved builtin tool name.")
        }
        val decision = safetyPolicy.evaluate(normalized.command)
        if (!decision.allowed) {
            return CustomToolValidation("command", decision.reason)
        }
        if (!overwrite && list().any { it.name == normalized.name }) {
            return CustomToolValidation("name", "Already exists in custom_tools.")
        }
        return null
    }

    private fun CustomTool.normalized(): CustomTool {
        return copy(
            name = name.trim(),
            description = description.trim(),
            command = command.trim(),
        )
    }

    companion object {
        private val NAME_PATTERN = Regex("^[a-zA-Z_][a-zA-Z0-9_]{1,63}$")
    }
}

class BuiltinToolApi internal constructor(
    private val repo: XRepo,
    private val registry: BuiltinToolRegistry = BuiltinToolRegistry.default(),
) {
    suspend fun list(): List<BuiltinToolSetting> {
        val flags = LocalSettingsCodec.parseBuiltinFlags(repo.readLocal())
        return registry.all()
            .sortedBy { it.name }
            .map { tool ->
                BuiltinToolSetting(
                    name = tool.name,
                    description = tool.description,
                    enabled = flags[tool.name] ?: tool.defaultEnabled,
                )
            }
    }

    suspend fun enabled(): List<BuiltinToolSetting> {
        return list().filter { it.enabled }
    }

    suspend fun setEnabled(name: String, enabled: Boolean): CustomToolValidation? {
        if (registry.find(name) == null) {
            return CustomToolValidation("name", "Builtin tool is not registered.")
        }
        repo.updateLocal { settings ->
            LocalSettingsCodec.withBuiltinFlag(settings, name, enabled)
        }
        return null
    }
}
