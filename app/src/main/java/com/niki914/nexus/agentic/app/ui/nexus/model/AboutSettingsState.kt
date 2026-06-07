package com.niki914.nexus.agentic.app.ui.nexus.model

import androidx.annotation.StringRes
import com.niki914.nexus.agentic.app.R
import com.niki914.nexus.cb.ComposeMVIViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

enum class AboutSettingsItemId {
    AuthorHomepage,
    Github,
    Afdian,
    Telegram,
    FeatureFeedback,
    BugFeedback,
}

data class AboutSettingsItemUiState(
    val id: AboutSettingsItemId,
    @StringRes val titleRes: Int,
    val uri: String?,
) {
    val canOpen: Boolean
        get() = uri != null || id == AboutSettingsItemId.FeatureFeedback || id == AboutSettingsItemId.BugFeedback
}

data class AboutSettingsUiState(
    val items: List<AboutSettingsItemUiState> = emptyList(),
)

sealed interface AboutSettingsIntent {
    data class OpenItem(val id: AboutSettingsItemId) : AboutSettingsIntent
}

sealed interface AboutSettingsEffect {
    data class OpenUri(val uri: String) : AboutSettingsEffect
}

class AboutSettingsViewModel :
    ComposeMVIViewModel<AboutSettingsIntent, AboutSettingsUiState, AboutSettingsEffect>() {

    override fun initUiState(): AboutSettingsUiState {
        return AboutSettingsUiState(items = aboutSettingsItems())
    }

    override suspend fun handleIntent(intent: AboutSettingsIntent) {
        when (intent) {
            is AboutSettingsIntent.OpenItem -> openItem(intent.id)
        }
    }

    private fun openItem(id: AboutSettingsItemId) {
        currentState.items
            .firstOrNull { it.id == id }
            ?.uri
            ?.let { uri ->
                sendEffect(AboutSettingsEffect.OpenUri(uri))
            }
    }
}

private fun aboutSettingsItems(): List<AboutSettingsItemUiState> {
    return listOf(
        AboutSettingsItemUiState(
            id = AboutSettingsItemId.AuthorHomepage,
            titleRes = R.string.ui_settings_about_author_homepage,
            uri = "https://github.com/niki914",
        ),
        AboutSettingsItemUiState(
            id = AboutSettingsItemId.Github,
            titleRes = R.string.ui_settings_about_github,
            uri = "https://github.com/niki914/agentic-nexus",
        ),
        AboutSettingsItemUiState(
            id = AboutSettingsItemId.Afdian,
            titleRes = R.string.ui_settings_about_afdian,
            uri = "https://afdian.com/a/niki914",
        ),
        AboutSettingsItemUiState(
            id = AboutSettingsItemId.Telegram,
            titleRes = R.string.ui_settings_about_telegram,
            uri = "https://t.me/+ZPX2xtSl6RwyZGNl",
        ),
        AboutSettingsItemUiState(
            id = AboutSettingsItemId.FeatureFeedback,
            titleRes = R.string.ui_settings_about_feedback_feature,
            uri = issueUri(
                title = FEATURE_FEEDBACK_TITLE,
                body = FEATURE_FEEDBACK_BODY,
            )
        ),
        AboutSettingsItemUiState(
            id = AboutSettingsItemId.BugFeedback,
            titleRes = R.string.ui_settings_about_feedback_bug,
            uri = issueUri(
                title = BUG_FEEDBACK_TITLE,
                body = BUG_FEEDBACK_BODY,
            )
        ),
    )
}

private fun issueUri(title: String, body: String): String {
    return "$ISSUES_NEW_URI?title=${encodeUriQueryValue(title)}&body=${encodeUriQueryValue(body)}"
}

private fun encodeUriQueryValue(value: String): String {
    return URLEncoder.encode(value, StandardCharsets.UTF_8.name())
        .replace("+", "%20")
}

private const val ISSUES_NEW_URI = "https://github.com/niki914/nexus/issues/new"

private const val FEATURE_FEEDBACK_TITLE = "[FEATURE] "
private const val FEATURE_FEEDBACK_BODY = """## 功能建议

## 使用场景

## 预期效果
"""

private const val BUG_FEEDBACK_TITLE = "[BUG] "
private const val BUG_FEEDBACK_BODY = """## 问题描述

## 复现步骤
1. 
2. 
3. 

## 预期行为

## 实际行为

## 设备信息
- Android 版本：
- 助手（小布或其他）
- 模块版本：

## 更多

可以补充日志、截图录屏等信息，以便于我们理解
"""
