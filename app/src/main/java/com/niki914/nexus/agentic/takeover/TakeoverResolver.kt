package com.niki914.nexus.agentic.takeover

import com.niki914.nexus.agentic.runtime.settings.model.RuntimeTakeoverRule
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeTakeoverTarget
import com.niki914.nexus.agentic.util.TextPatternMatcher

data class TakeoverDecision(
    val target: RuntimeTakeoverTarget,
    val matchedRuleId: String? = null,
    val matchedRuleName: String? = null,
) {
    companion object {
        val Default: TakeoverDecision = TakeoverDecision(RuntimeTakeoverTarget.NEXUS)
    }
}

object TakeoverResolver {
    fun resolve(query: String, rules: List<RuntimeTakeoverRule>): TakeoverDecision {
        val matchedRule = rules.firstOrNull { rule ->
            rule.enabled && TextPatternMatcher.matchesAny(query, rule.patterns)
        } ?: return TakeoverDecision.Default

        return TakeoverDecision(
            target = matchedRule.target,
            matchedRuleId = matchedRule.id,
            matchedRuleName = matchedRule.name,
        )
    }
}
