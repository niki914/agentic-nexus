package com.niki914.nexus.agentic.app.ui.nexus.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.niki914.nexus.agentic.app.R
import com.niki914.nexus.agentic.app.ui.infra.component.TintLiquidButton
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@Composable
fun ConfigurePageContent(
    topPadding: Dp,
    hazeState: HazeState,
    buttonDarkContainerColor: Color = MaterialTheme.colorScheme.primary,
    buttonLightContainerColor: Color = MaterialTheme.colorScheme.primary,
    buttonDarkContentColor: Color = MaterialTheme.colorScheme.onPrimary,
    buttonLightContentColor: Color = MaterialTheme.colorScheme.onPrimary,
    onComplete: () -> Unit,
) {
    ConfigurePageContent(
        topPadding = topPadding,
        hazeState = hazeState,
        headline = stringResource(R.string.nexus_configure_headline),
        description = stringResource(R.string.nexus_configure_description),
        buttonText = stringResource(R.string.nexus_configure_complete),
        buttonDarkContainerColor = buttonDarkContainerColor,
        buttonLightContainerColor = buttonLightContainerColor,
        buttonDarkContentColor = buttonDarkContentColor,
        buttonLightContentColor = buttonLightContentColor,
        onComplete = onComplete,
    )
}

@Composable
internal fun ConfigurePageContent(
    topPadding: Dp,
    hazeState: HazeState,
    headline: String,
    description: String,
    buttonText: String,
    buttonDarkContainerColor: Color = MaterialTheme.colorScheme.primary,
    buttonLightContainerColor: Color = MaterialTheme.colorScheme.primary,
    buttonDarkContentColor: Color = MaterialTheme.colorScheme.onPrimary,
    buttonLightContentColor: Color = MaterialTheme.colorScheme.onPrimary,
    onComplete: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .hazeSource(hazeState)
            .padding(top = topPadding)
            .padding(horizontal = 24.dp, vertical = 24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = headline,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            TintLiquidButton(
                text = buttonText,
                darkContainerColor = buttonDarkContainerColor,
                lightContainerColor = buttonLightContainerColor,
                darkContentColor = buttonDarkContentColor,
                lightContentColor = buttonLightContentColor,
                onClick = onComplete,
            )
        }
    }
}
