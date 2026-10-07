package com.niki914.zafiro.app.ui.route

import androidx.compose.runtime.Composable
import com.niki914.zafiro.app.ui.content.TextActionDetailContent
import com.niki914.zafiro.app.ui.nav.TextActionDetailPage

@Composable
internal fun TextActionDetailRoute(
    page: TextActionDetailPage,
    onBack: () -> Unit,
) {
    TextActionDetailContent(
        page = page,
        onBack = onBack,
    )
}
