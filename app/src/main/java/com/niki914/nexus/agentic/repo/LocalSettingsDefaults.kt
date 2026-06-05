package com.niki914.nexus.agentic.repo

import com.niki914.nexus.agentic.mod.LocalSettings
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeCustomTool
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeExecutionRule
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeExecutionRuleEnabledMode

internal object LocalSettingsDefaults {
    const val DEFAULT_SYSTEM_PROMPT =
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
    """

    private val defaultCustomTools = listOf(
        RuntimeCustomTool(
            name = "launch_wechat",
            description = "启动微信",
            command = "am start -n com.tencent.mm/com.tencent.mm.ui.LauncherUI",
            enabled = true,
        )
    )

    val defaultExecutionRules = listOf(
        RuntimeExecutionRule(
            id = "builtin-dangerous-delete",
            name = "危险删改",
            enabledMode = RuntimeExecutionRuleEnabledMode.LOCKED_ONLY,
            patterns = listOf(
                "\\brm\\s+-rf\\b",
                "\\brm\\s+-(?=[^\\s]*r)(?=[^\\s]*f)[^\\s]*\\b",
                "\\brm\\s+-r\\s+-f\\b",
                "\\brm\\s+(?=[^\\n]*--recursive\\b)(?=[^\\n]*--force\\b)[^\\n]*",
                "\\brm\\s+(?=[^\\n]*-(?:[^\\s-]*r[^\\s-]*|-[^-\\s]*recursive)\\b)(?=[^\\n]*-(?:[^\\s-]*f[^\\s-]*|-[^-\\s]*force)\\b)[^\\n]*",
                "\\bmkfs\\b",
            ),
        ),
        RuntimeExecutionRule(
            id = "builtin-uninstall",
            name = "卸载相关",
            enabledMode = RuntimeExecutionRuleEnabledMode.ALWAYS,
            patterns = listOf("\\bpm\\s+uninstall\\b", "\\bcmd\\s+package\\s+uninstall\\b"),
        ),
        RuntimeExecutionRule(
            id = "builtin-privileged",
            name = "高危提权",
            enabledMode = RuntimeExecutionRuleEnabledMode.ALWAYS,
            patterns = listOf("\\bsu\\b", "\\bsetprop\\b", "\\bdd\\b", "\\breboot\\b"),
        ),
    )

    fun applyTo(settings: LocalSettings): LocalSettings {
        return LocalSettingsCodec.withExecutionRules(
            settings = LocalSettingsCodec.withCustomTools(
                settings = LocalSettingsCodec.withPrompt(
                    settings = settings,
                    prompt = DEFAULT_SYSTEM_PROMPT.trimIndent(),
                ),
                tools = defaultCustomTools,
            ),
            rules = defaultExecutionRules,
        )
    }
}
