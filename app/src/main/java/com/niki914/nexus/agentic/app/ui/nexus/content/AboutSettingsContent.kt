package com.niki914.nexus.agentic.app.ui.nexus.content

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.niki914.nexus.agentic.app.BuildConfig
import com.niki914.nexus.agentic.app.R
import com.niki914.nexus.agentic.app.ui.infra.ProvideLiquidScreenContentForPreview
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsGroupCard
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsItemDivider
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsListItem
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsListPageContent
import com.niki914.nexus.agentic.app.ui.infra.nav.pageViewModel
import com.niki914.nexus.agentic.app.ui.nexus.model.AboutSettingsEffect
import com.niki914.nexus.agentic.app.ui.nexus.model.AboutSettingsIntent
import com.niki914.nexus.agentic.app.ui.nexus.model.AboutSettingsItemId
import com.niki914.nexus.agentic.app.ui.nexus.model.AboutSettingsItemUiState
import com.niki914.nexus.agentic.app.ui.nexus.model.AboutSettingsUiState
import com.niki914.nexus.agentic.app.ui.nexus.model.AboutSettingsViewModel
import com.niki914.nexus.cb.BaseTheme

@Composable
fun AboutSettingsContent() {
    val viewModel = pageViewModel<AboutSettingsViewModel>()
    val uiState by viewModel.uiStateFlow.collectAsState()
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(viewModel, uriHandler) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is AboutSettingsEffect.OpenUri -> uriHandler.openUri(effect.uri)
            }
        }
    }

    AboutSettingsContentBody(
        uiState = uiState,
        onItemClick = { item ->
            viewModel.sendIntent(AboutSettingsIntent.OpenItem(item.id))
        },
    )
}

@Composable
private fun AboutSettingsContentBody(
    uiState: AboutSettingsUiState,
    onItemClick: (AboutSettingsItemUiState) -> Unit,
) {
    SettingsListPageContent(
        description = stringResource(R.string.ui_settings_about_description),
    ) {
        SettingsGroupCard {
            uiState.items.forEach { item ->
                AboutSettingsListItem(
                    item = item,
                    onClick = {
                        onItemClick(item)
                    },
                )
                SettingsItemDivider()
            }
            SettingsListItem(
                title = stringResource(R.string.ui_settings_about_version),
                currentState = BuildConfig.VERSION_NAME,
            )
        }
    }
}

@Composable
private fun AboutSettingsListItem(
    item: AboutSettingsItemUiState,
    onClick: () -> Unit,
) {
    SettingsListItem(
        title = stringResource(item.titleRes),
        showChevron = item.canOpen,
        leadingContent = {
            AboutSettingsLeadingIcon(id = item.id)
        },
        onClick = if (item.canOpen) onClick else null,
    )
}

@Composable
private fun AboutSettingsLeadingIcon(id: AboutSettingsItemId) {
    when (id) {
        AboutSettingsItemId.AuthorHomepage,
        AboutSettingsItemId.Github -> AboutSettingsDrawableIcon(drawableRes = R.drawable.github)

        AboutSettingsItemId.Afdian -> AboutSettingsDrawableIcon(drawableRes = R.drawable.aifadian)
        AboutSettingsItemId.Telegram -> AboutSettingsDrawableIcon(drawableRes = R.drawable.telegram)
        AboutSettingsItemId.FeatureFeedback -> AboutSettingsIcon(imageVector = Icons.Default.Feedback)
        AboutSettingsItemId.BugFeedback -> AboutSettingsIcon(imageVector = Icons.Default.BugReport)
    }
}

@Composable
private fun AboutSettingsIcon(imageVector: ImageVector) {
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp),
    )
}

@Composable
private fun AboutSettingsDrawableIcon(@DrawableRes drawableRes: Int) {
    Icon(
        painter = painterResource(drawableRes),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp),
    )
}

@Preview(name = "About Settings Light", showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun AboutSettingsLightPreview() {
    BaseTheme(darkTheme = false, dynamicColor = false) {
        Surface {
            ProvideLiquidScreenContentForPreview(topPadding = 0.dp) {
                AboutSettingsContentBody(
                    uiState = previewAboutSettingsUiState(),
                    onItemClick = {},
                )
            }
        }
    }
}

@Preview(
    name = "About Settings Dark",
    showBackground = true,
    widthDp = 420,
    heightDp = 900,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AboutSettingsDarkPreview() {
    BaseTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            ProvideLiquidScreenContentForPreview(topPadding = 0.dp) {
                AboutSettingsContentBody(
                    uiState = previewAboutSettingsUiState(),
                    onItemClick = {},
                )
            }
        }
    }
}

private fun previewAboutSettingsUiState(): AboutSettingsUiState {
    return AboutSettingsUiState(
        items = listOf(
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
                uri = null,
            ),
            AboutSettingsItemUiState(
                id = AboutSettingsItemId.Telegram,
                titleRes = R.string.ui_settings_about_telegram,
                uri = null,
            ),
            AboutSettingsItemUiState(
                id = AboutSettingsItemId.FeatureFeedback,
                titleRes = R.string.ui_settings_about_feedback_feature,
                uri = "https://github.com/niki914/nexus/issues/new",
            ),
            AboutSettingsItemUiState(
                id = AboutSettingsItemId.BugFeedback,
                titleRes = R.string.ui_settings_about_feedback_bug,
                uri = "https://github.com/niki914/nexus/issues/new",
            ),
        )
    )
}
