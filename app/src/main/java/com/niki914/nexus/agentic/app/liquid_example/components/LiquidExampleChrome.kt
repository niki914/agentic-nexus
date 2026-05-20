package com.niki914.nexus.agentic.app.liquid_example.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/**
 * 多页面导航顶栏：左右上角按钮 + 居中标题，齐平于状态栏下方。
 * 灵感来源：librepods 的 StyledScaffold（去除整页阴影/圆角与胶囊）。
 */
@Composable
fun LiquidExampleChrome(
    title: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    val hazeState = rememberHazeState(blurEnabled = true)
    val chromeBackdrop = rememberLayerBackdrop()
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(modifier.fillMaxSize()) {
        // 内容层：作为 haze source，让顶栏可以模糊到滚动中的内容
        Box(
            Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
        ) {
            content()
        }

        // 顶栏背景：渐进毛玻璃，向下淡出
        Box(
            Modifier
                .zIndex(2f)
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(topInset + 56.dp)
                .layerBackdrop(chromeBackdrop)
                .hazeEffect(state = hazeState) {
                    tints = listOf(
                        HazeTint(
                            if (isDarkTheme) {
                                Color.Black.copy(alpha = 0.42f)
                            } else {
                                Color.White.copy(alpha = 0.55f)
                            }
                        )
                    )
                    progressive = HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f)
                }
        )

        // 顶栏前景：左按钮 / 标题 / 右按钮，齐平于屏幕上方
        Row(
            modifier = Modifier
                .zIndex(3f)
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(topInset + 56.dp)
                .padding(top = topInset)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StyledIconButton(
                onClick = onPrev,
                backdrop = chromeBackdrop,
                icon = "‹"
            )

            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDarkTheme) Color.White else Color.Black
                ),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            StyledIconButton(
                onClick = onNext,
                backdrop = chromeBackdrop,
                icon = "›"
            )
        }
    }
}
