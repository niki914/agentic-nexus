package com.niki914.nexus.agentic.app.ui.nexus.route

import androidx.compose.runtime.Composable
import com.niki914.nexus.agentic.app.ui.nexus.content.ExecutionRuleDetailContent
import com.niki914.nexus.agentic.app.ui.nexus.nav.ExecutionRuleDetailPage

@Composable
internal fun ExecutionRuleDetailRoute(
    page: ExecutionRuleDetailPage,
) {
    ExecutionRuleDetailContent(
        page = page,
    )
}
