package com.niki914.nexus.agentic.app.ui.nexus.content

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SettingsDetailPageContentSourceTest {
    @Test
    fun settingsDetailPage_routesMemoryAndExecutionRulesToRealPages() {
        val source = File(
            "src/main/java/com/niki914/nexus/agentic/app/ui/nexus/content/SettingsDetailPageContent.kt"
        ).readText()

        val memoryBranch = source.branchFor("Memory")
        val executionRulesBranch = source.branchFor("ExecutionRules")

        assertTrue(memoryBranch.contains("MemorySettingsContent()"))
        assertFalse(memoryBranch.contains("TODOPageContent()"))
        assertTrue(executionRulesBranch.contains("ExecutionRulesSettingsContent("))
        assertTrue(executionRulesBranch.contains("ExecutionRuleDetailPage"))
        assertFalse(executionRulesBranch.contains("TODOPageContent()"))
    }

    private fun String.branchFor(groupName: String): String {
        val pattern = Regex(
            pattern = """if \(group == NexusSettingsGroup\.$groupName\) \{([\s\S]*?)\n    \}""",
        )
        return requireNotNull(pattern.find(this)) {
            "Missing branch for NexusSettingsGroup.$groupName"
        }.groupValues[1]
    }
}
