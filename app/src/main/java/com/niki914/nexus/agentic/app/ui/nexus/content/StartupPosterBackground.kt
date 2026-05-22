package com.niki914.nexus.agentic.app.ui.nexus.content

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun StartupPosterBackground(
    modifier: Modifier = Modifier,
) {
    val isDarkTheme = isSystemInDarkTheme()
    val backgroundColors = if (isDarkTheme) {
        listOf(
            Color(0xFF121624),
            Color(0xFF0B0C12),
            Color(0xFF09090B),
        )
    } else {
        listOf(
            Color(0xFFF7FAFF),
            Color(0xFFE9F1FF),
            Color(0xFFF5F8FF),
        )
    }
    val topLeftColor = if (isDarkTheme) Color(0x665B7CFF) else Color(0x8A6F8EFF)
    val topRightColor = if (isDarkTheme) Color(0x334FE7FF) else Color(0x8066C9FF)
    val horizonGlowColor = if (isDarkTheme) Color(0x146FE8FF) else Color(0x6B79D6FF)
    val bottomGlowColor = if (isDarkTheme) Color(0x18FFFFFF) else Color(0x59FFFFFF)
    val centerMistColor = if (isDarkTheme) Color(0x12B5F6FF) else Color(0x5699DAFF)
    val edgeTintColor = if (isDarkTheme) Color.Transparent else Color(0x180E2247)

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val backgroundBrush = Brush.verticalGradient(
                    colors = backgroundColors,
                )
                val topLeftGlow = Brush.radialGradient(
                    colors = listOf(topLeftColor, Color.Transparent),
                    center = Offset(size.width * 0.18f, size.height * 0.18f),
                    radius = size.minDimension * 0.42f,
                )
                val topRightGlow = Brush.radialGradient(
                    colors = listOf(topRightColor, Color.Transparent),
                    center = Offset(size.width * 0.82f, size.height * 0.2f),
                    radius = size.minDimension * 0.46f,
                )
                val bottomGlow = Brush.radialGradient(
                    colors = listOf(bottomGlowColor, Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.88f),
                    radius = size.minDimension * 0.56f,
                )
                val horizonGlow = Brush.radialGradient(
                    colors = listOf(horizonGlowColor, Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.42f),
                    radius = size.minDimension * 0.5f,
                )
                val edgeTint = Brush.verticalGradient(
                    colors = listOf(edgeTintColor, Color.Transparent, edgeTintColor),
                )
                onDrawBehind {
                    drawRect(backgroundBrush)
                    drawRect(topLeftGlow)
                    drawRect(topRightGlow)
                    drawRect(horizonGlow)
                    drawRect(bottomGlow)
                    drawRect(edgeTint)
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(if (isDarkTheme) 72.dp else 88.dp)
                .drawWithCache {
                    val centerMist = Brush.radialGradient(
                        colors = listOf(centerMistColor, Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 0.36f),
                        radius = size.minDimension * if (isDarkTheme) 0.3f else 0.38f,
                    )
                    onDrawBehind {
                        drawRect(centerMist)
                    }
                }
        )
    }
}
