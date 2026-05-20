package com.niki914.nexus.agentic.app.ui.infra

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
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
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.rememberHazeState

@Composable
fun LiquidScreen(
    state: LiquidScreenState,
    modifier: Modifier = Modifier,
    leftButton: (@Composable () -> Unit)? = null,
    rightButton: (@Composable () -> Unit)? = null,
    content: @Composable (hazeState: HazeState) -> Unit,
) {
    val isDarkTheme = isSystemInDarkTheme()
    val hazeState = rememberHazeState(blurEnabled = true)
    val chromeBackdrop = rememberLayerBackdrop()
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val actionBarHeight = topInset + 56.dp

    SideEffect { state.setActionBarHeight(actionBarHeight) }

    Box(modifier.fillMaxSize()) {
        // Layer 1: page content owns the haze source placement.
        content(hazeState)

        // Layer 2: action bar progressive blur background
        Box(
            Modifier
                .zIndex(2f)
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(actionBarHeight)
                .layerBackdrop(chromeBackdrop)
                .hazeEffect(state = hazeState) {
                    tints = listOf(
                        HazeTint(
                            if (isDarkTheme) Color.Black else Color.White
                        )
                    )
                    progressive = HazeProgressive.verticalGradient(
                        startIntensity = 1f,
                        endIntensity = 0f
                    )
                }
        )

        // Layer 3: action bar foreground
        Box(
            modifier = Modifier
                .zIndex(3f)
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(actionBarHeight)
                .padding(top = topInset)
                .padding(horizontal = 4.dp),
        ) {
            // Title — always centered in the full bar width
            val buttonDuration = 280
            AnimatedContent(
                targetState = state.title,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 48.dp),
                transitionSpec = {
                    when (state.titleDirection) {
                        TitleDirection.Forward -> {
                            (slideInHorizontally { it } + fadeIn(tween(buttonDuration))) togetherWith
                                (slideOutHorizontally { -it } + fadeOut(tween(buttonDuration)))
                        }
                        TitleDirection.Back -> {
                            (slideInHorizontally { -it } + fadeIn(tween(buttonDuration))) togetherWith
                                (slideOutHorizontally { it } + fadeOut(tween(buttonDuration)))
                        }
                        TitleDirection.None -> {
                            ContentTransform(
                                targetContentEnter = EnterTransition.None,
                                initialContentExit = ExitTransition.None,
                                sizeTransform = SizeTransform(clip = false),
                            )
                        }
                    }.using(SizeTransform(clip = false))
                },
                label = "title",
            ) { title ->
                Text(
                    text = title,
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDarkTheme) Color.White else Color.Black
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Left button
            // TODO copy librepods
            AnimatedVisibility(
                visible = state.showLeftButton,
                modifier = Modifier.align(Alignment.CenterStart),
                enter = fadeIn(tween(buttonDuration, easing = LinearOutSlowInEasing)) +
                    scaleIn(
                        initialScale = 0f,
                        animationSpec = tween(buttonDuration, easing = LinearOutSlowInEasing)
                    ),
                exit = fadeOut(tween(buttonDuration, easing = LinearOutSlowInEasing)) +
                    scaleOut(
                        targetScale = 0f,
                        animationSpec = tween(buttonDuration, easing = LinearOutSlowInEasing)
                    ),
            ) {
                ActionBarButton(
                    onClick = { state.onLeftClick?.invoke() },
                    backdrop = chromeBackdrop,
                    content = leftButton ?: {
                        Text(
                            text = "‹",
                            style = TextStyle(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Normal,
                                color = if (isDarkTheme) Color.White else Color.Black,
                            )
                        )
                    },
                )
            }

            // Right button
            AnimatedVisibility(
                visible = state.showRightButton,
                modifier = Modifier.align(Alignment.CenterEnd),
                enter = fadeIn(tween(buttonDuration, easing = LinearOutSlowInEasing)) +
                    scaleIn(
                        initialScale = 0f,
                        animationSpec = tween(buttonDuration, easing = LinearOutSlowInEasing)
                    ),
                exit = fadeOut(tween(buttonDuration, easing = LinearOutSlowInEasing)) +
                    scaleOut(
                        targetScale = 0f,
                        animationSpec = tween(buttonDuration, easing = LinearOutSlowInEasing)
                    ),
            ) {
                ActionBarButton(
                    onClick = { state.onRightClick?.invoke() },
                    backdrop = chromeBackdrop,
                    content = rightButton ?: {
                        Text(
                            text = "›",
                            style = TextStyle(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Normal,
                                color = if (isDarkTheme) Color.White else Color.Black,
                            )
                        )
                    },
                )
            }
        }
    }
}
