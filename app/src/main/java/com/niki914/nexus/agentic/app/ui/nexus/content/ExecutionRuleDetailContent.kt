package com.niki914.nexus.agentic.app.ui.nexus.content

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.niki914.nexus.agentic.app.R
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsListPageContent
import com.niki914.nexus.agentic.app.ui.nexus.nav.ExecutionRuleDetailPage

@Composable
@Suppress("UNUSED_PARAMETER")
fun ExecutionRuleDetailContent(
    page: ExecutionRuleDetailPage,
) {
    SettingsListPageContent(
        description = stringResource(R.string.execution_rules_page_description),
    ) {}
}
