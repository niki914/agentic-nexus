package com.niki914.nexus.agentic.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import com.niki914.nexus.agentic.mod.LocalSettings
import com.niki914.nexus.agentic.mod.XService
import com.niki914.nexus.agentic.repo.XRepoRuntimeGateway
import com.niki914.nexus.agentic.runtime.settings.RuntimeEnvironment
import com.niki914.nexus.h.util.xlog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class App : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        RuntimeEnvironment.install(XRepoRuntimeGateway())
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        DynamicColors.applyToActivitiesIfAvailable(this)
        applicationScope.launch {
            val existingSettings = XService.getLocalSettings(this@App)
            if (!shouldSeedDefaultSettings(existingSettings)) {
                return@launch
            }
            XService.putLocalSettings(
                this@App,
                mergeDefaultLocalSettings(existingSettings),
            )
        }
    }

    private fun shouldSeedDefaultSettings(settings: LocalSettings): Boolean {
        return true
        return settings.endpoint.isBlank() &&
                settings.model.isBlank() &&
                settings.customTools == null
    }

    private fun mergeDefaultLocalSettings(existingSettings: LocalSettings): LocalSettings {
        val mergedProps = existingSettings.props.toMutableMap()
        buildDefaultProps().forEach { (key, value) ->
            if (shouldApplyDefaultValue(existingSettings, key)) {
                mergedProps[key] = value
                xlog("mergeDefaultLocalSettings put: $key, value: $value")
            } else {
                xlog("mergeDefaultLocalSettings skip putting $key, curr value: ${existingSettings.apiKey}")
            }
        }
        return LocalSettings(JsonObject(mergedProps))
    }

    private fun shouldApplyDefaultValue(
        settings: LocalSettings,
        key: String,
    ): Boolean {
        return when (key) {
            "endpoint" -> settings.endpoint.isBlank()
            "api_key" -> settings.apiKey.isBlank()
            "model" -> settings.model.isBlank()
            "prompt" -> settings.prompt.isBlank()
            "proxy" -> settings.proxy.isBlank()
            "takeover_keywords" -> settings.takeoverKeywords.isEmpty()
            "mcp_servers" -> settings.mcpServers == null
            "custom_tools" -> settings.customTools == null
            else -> settings.props[key] == null
        }
    }

    private fun buildDefaultProps(): Map<String, JsonElement> {
        return mapOf(
            "endpoint" to JsonPrimitive("https://api.xiaomimimo.com/v1/chat/completions"),
            "api_key" to JsonPrimitive("sk-czrf8ciubfm5050mqfm4a34szxq4v0jzpu8brle8vhoiwb5l"),
            "model" to JsonPrimitive("mimo-v2.5"),
            "prompt" to JsonPrimitive(
                """
                <system_identity>
                You are Nexus, an assistant created by niki914.
                </system_identity>
                <execution_rules>
                1. Immediate Acknowledgment: You MUST output a text response to the user explaining your action BEFORE or CONCURRENTLY with invoking any tools.
                2. No Silent Batching: NEVER execute multiple tool calls consecutively in the background without user feedback.
                3. Direct Execution: If the user explicitly requests a known action (e.g., "打开微信"), execute the tool immediately. DO NOT ask for redundant permission.
                4. Permission for Ambiguity: Only ask for user confirmation if the action is destructive or lacks clear context.
                5. Fail Fast on Tool Errors: If a tool fails or cannot complete the task, DO NOT exhaustively guess or try other tools. Stop immediately, report the failure, and ASK the user for permission to try a specific alternative.
                </execution_rules>
                <examples>
                === Example 1: Direct Command ===
                User: 打开微信
                ✅ Nexus: 好的，正在为您打开微信。[tool_call: launch_wechat]

                === Example 2: Negative Example (Silent Batching) ===
                User: 帮我完成日常签到
                ❌ Nexus: [tool_call: A] -> [tool_call: B] -> 签到完成。
                ✅ Nexus: 好的，我将为您执行签到流程。首先执行A... [tool_call: A]

                === Example 3: Fail Fast ===
                User: 打开微信
                ❌ Nexus: [tool_call: launch_wechat returns error]
                ❌ Nexus: (silently tries tool B、C、D、)
                ✅ Nexus: 抱歉，打开微信失败。是否需要我尝试通过 `run_command` 尝试打开？
                </examples>
                """.trimIndent()
            ),
            "proxy" to JsonPrimitive(""),
            "takeover_keywords" to JsonArray(
                emptyList()
//              listOf(JsonPrimitive("闹钟"), JsonPrimitive("清理"))
            ),
            "custom_tools" to JsonArray(
                listOf(
                    JsonObject(
                        mapOf(
                            "name" to JsonPrimitive("launch_wechat"),
                            "description" to JsonPrimitive("启动微信"),
                            "enabled" to JsonPrimitive(true),
                            "command" to JsonPrimitive("am start -n com.tencent.mm/com.tencent.mm.ui.LauncherUI"),
                        )
                    )
                )
            ),
            "mcp_servers" to JsonArray((0..10).map { mockMCP(it) })
        )
    }
}

fun mockMCP(num: Int) = JsonObject(
    mapOf(
        "name" to JsonPrimitive("mcp-server-$num"),
        "url" to JsonPrimitive("http://127.0.0.1:51338/mcp"),
        "enabled" to JsonPrimitive(true)
    )
)
