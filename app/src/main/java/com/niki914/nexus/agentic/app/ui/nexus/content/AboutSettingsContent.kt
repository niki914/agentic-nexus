package com.niki914.nexus.agentic.app.ui.nexus.content

import android.content.res.Configuration
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
                    AboutSettingsIcon(imageVector = Icons.Default.Person)
                },
                onClick = {},
            )
            SettingsItemDivider()
            SettingsListItem(
                title = stringResource(R.string.ui_settings_about_github),
                showChevron = true,
                leadingContent = {
                    AboutSettingsIcon(imageVector = Icons.Default.Code)
                },
                onClick = {},
            )
            SettingsItemDivider()
            SettingsListItem(
                title = stringResource(R.string.ui_settings_about_afdian),
                showChevron = true,
                leadingContent = {
                    AboutSettingsIcon(imageVector = Icons.Default.Favorite)
                },
                onClick = {},
            )
            SettingsItemDivider()
            SettingsListItem(
                title = stringResource(R.string.ui_settings_about_telegram),
                showChevron = true,
                leadingContent = {
                    AboutSettingsIcon(imageVector = Icons.Default.Groups)
                },
                onClick = {},
            )
            SettingsItemDivider()
            SettingsListItem(
                title = stringResource(R.string.ui_settings_about_version),
                currentState = BuildConfig.VERSION_NAME,
                leadingContent = {
                    AboutSettingsIcon(imageVector = Icons.Default.Info)
                },
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
