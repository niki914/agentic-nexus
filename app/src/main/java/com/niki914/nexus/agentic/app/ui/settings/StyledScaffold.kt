package com.niki914.nexus.agentic.app.ui.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.rememberHazeState

@Composable
fun StyledScaffold(
    title: String,
    content: @Composable (spacerValue: Dp, hazeState: HazeState, bottomPadding: Dp) -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    val hazeState = rememberHazeState(blurEnabled = true)

    Scaffold(
        containerColor = if (isDarkTheme) Color(0xFF000000) else Color(0xFFF2F2F7),
        modifier = Modifier
            .then(if (!isDarkTheme) Modifier.shadow(elevation = 36.dp, shape = RoundedCornerShape(52.dp), ambientColor = Color.Black, spotColor = Color.Black) else Modifier)
            .clip(RoundedCornerShape(52.dp))
    ) { paddingValues ->
        val topPadding = paddingValues.calculateTopPadding()
        val bottomPadding = paddingValues.calculateBottomPadding() + 16.dp
        val startPadding = paddingValues.calculateLeftPadding(LocalLayoutDirection.current)
        val endPadding = paddingValues.calculateRightPadding(LocalLayoutDirection.current)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = startPadding, end = endPadding)
        ) {
            val backdrop = rememberLayerBackdrop()
            Box(
                modifier = Modifier
                    .zIndex(2f)
                    .height(64.dp + topPadding)
                    .fillMaxWidth()
                    .layerBackdrop(backdrop)
                    .hazeEffect(state = hazeState) {
                        tints = listOf(HazeTint(color = if (isDarkTheme) Color.Black else Color.White))
                        progressive = HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f)
                    }
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Spacer(modifier = Modifier.height(topPadding + 12.dp))
                    Text(
                        text = title,
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDarkTheme) Color.White else Color.Black,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }

            content(topPadding + 64.dp, hazeState, bottomPadding + 12.dp)
        }
    }
}
