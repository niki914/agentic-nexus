package com.niki914.nexus.agentic.app.ui.nexus.content

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niki914.nexus.agentic.app.R
import com.niki914.nexus.agentic.app.ui.infra.component.TintLiquidButton
import com.niki914.nexus.agentic.app.ui.infra.liquidScreenTopPadding
import com.niki914.nexus.agentic.app.ui.nexus.model.StartupAssistantUi

@Composable
fun StartupPageContent(
    assistantUi: StartupAssistantUi,
    isLoading: Boolean = false,
    onContinue: () -> Unit,
) {
    val isDarkTheme = isSystemInDarkTheme()
    val headlineColor = if (isDarkTheme) Color.White.copy(alpha = 0.96f) else Color(0xFF101828)
    val statusColor = if (isDarkTheme) Color.White.copy(alpha = 0.74f) else Color(0xCC1D3557)
    val headlineShadow = if (isDarkTheme) {
        Shadow(
            color = Color.White.copy(alpha = 0.18f),
            blurRadius = 32f,
        )
    } else {
        Shadow(
            color = Color.White.copy(alpha = 0.65f),
            blurRadius = 24f,
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        StartupPosterBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = liquidScreenTopPadding())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = stringResource(R.string.ui_onboard_startup_headline),
                    style = TextStyle(
                        fontSize = 36.sp,
                        lineHeight = 40.sp,
                        letterSpacing = (-1.2).sp,
                        fontWeight = FontWeight.SemiBold,
                        color = headlineColor,
                        shadow = headlineShadow,
                    ),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(assistantUi.statusTextRes),
                    style = TextStyle(
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        letterSpacing = 0.1.sp,
                        fontWeight = FontWeight.Medium,
                        color = statusColor,
                    ),
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            TintLiquidButton(
                text = stringResource(assistantUi.buttonTextRes),
                onClick = onContinue,
                isLoading = isLoading,
                darkContainerColor = Color(0xFF8FD8FF),
                lightContainerColor = Color(0xFF4B6BFF),
            )
        }
    }
}
