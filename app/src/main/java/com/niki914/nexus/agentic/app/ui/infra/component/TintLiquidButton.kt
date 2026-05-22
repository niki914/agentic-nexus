package com.niki914.nexus.agentic.app.ui.infra.component

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.niki914.nexus.agentic.app.liquid_example.components.LiquidButton

@Composable
fun TintLiquidButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    darkContainerColor: Color = Color.Unspecified,
    lightContainerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
) {
    val isDarkTheme = isSystemInDarkTheme()
    val backdrop = rememberLayerBackdrop()
    val disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
    val disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    val resolvedContainerColor = when {
        darkContainerColor.isSpecified && lightContainerColor.isSpecified -> {
            if (isDarkTheme) darkContainerColor else lightContainerColor
        }

        darkContainerColor.isSpecified -> darkContainerColor
        lightContainerColor.isSpecified -> lightContainerColor
        else -> Color.Unspecified
    }
    val resolvedSurfaceColor = if (resolvedContainerColor.isSpecified) {
        resolvedContainerColor.copy(alpha = 0.18f)
    } else {
        Color.Unspecified
    }
    val resolvedContentColor = when {
        contentColor.isSpecified -> contentColor
        resolvedContainerColor.isSpecified -> {
            if (resolvedContainerColor.luminance() > 0.42f) Color(0xFF0F172A) else Color.White
        }
        else -> {
            if (isDarkTheme) MaterialTheme.colorScheme.onSurface else Color(0xFF111827)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(modifier),
        contentAlignment = Alignment.Center,
    ) {
        LiquidButton(
            onClick = if (enabled) onClick else { -> },
            backdrop = backdrop,
            modifier = Modifier.fillMaxWidth(),
            isInteractive = enabled,
            tint = if (enabled) resolvedContainerColor else disabledContainerColor,
            surfaceColor = if (enabled) resolvedSurfaceColor else Color.Transparent,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) resolvedContentColor else disabledContentColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
