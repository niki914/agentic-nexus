package com.niki914.nexus.agentic.app.ui.nexus.content

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.niki914.nexus.cb.BaseTheme

@Composable
fun AboutSettingsContent() {
    AboutSettingsContentBody()
}

@Composable
private fun AboutSettingsContentBody() {
    SettingsListPageContent(
        description = stringResource(R.string.ui_settings_about_description),
    ) {
        SettingsGroupCard {
            SettingsListItem(
                title = stringResource(R.string.ui_settings_about_author_homepage),
                showChevron = true,
                leadingContent = {
                    AboutSettingsDrawableIcon(drawableRes = R.drawable.github)
                },
                onClick = {},
            )
            SettingsItemDivider()
            SettingsListItem(
                title = stringResource(R.string.ui_settings_about_github),
                showChevron = true,
                leadingContent = {
                    AboutSettingsDrawableIcon(drawableRes = R.drawable.github)
                },
                onClick = {},
            )
            SettingsItemDivider()
            SettingsListItem(
                title = stringResource(R.string.ui_settings_about_afdian),
                showChevron = true,
                leadingContent = {
                    AboutSettingsDrawableIcon(drawableRes = R.drawable.aifadian)
                },
                onClick = {},
            )
            SettingsItemDivider()
            SettingsListItem(
                title = stringResource(R.string.ui_settings_about_telegram),
                showChevron = true,
                leadingContent = {
                    AboutSettingsDrawableIcon(drawableRes = R.drawable.telegram)
                },
                onClick = {},
            ) // TODO P0 反馈
            SettingsItemDivider()
            SettingsListItem(
                title = stringResource(R.string.ui_settings_about_version),
                currentState = BuildConfig.VERSION_NAME,
            )
        }
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
                AboutSettingsContentBody()
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
                AboutSettingsContentBody()
            }
        }
    }
}
