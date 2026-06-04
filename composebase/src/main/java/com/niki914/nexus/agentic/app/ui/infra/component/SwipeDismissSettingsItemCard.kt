package com.niki914.nexus.agentic.app.ui.infra.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.niki914.nexus.agentic.app.ui.infra.shape.G2CardShape
import kotlin.math.absoluteValue
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

@Composable
fun SwipeDismissSettingsItemCard(
    title: String,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    threshold: Dp = SwipeDismissSettingsItemDefaults.Threshold,
    iconAnchor: Dp = SwipeDismissSettingsItemDefaults.IconAnchor,
    dampingRange: Dp = SwipeDismissSettingsItemDefaults.DampingRange,
    dismissIcon: ImageVector = Icons.Default.Delete,
) {
    val density = LocalDensity.current
    val thresholdPx = with(density) { threshold.toPx() }
    val dampingRangePx = with(density) { dampingRange.toPx() }
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    var rawDistancePx by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(true) }
    var exitDirection by remember { mutableFloatStateOf(1f) }
    val progress = (rawDistancePx.absoluteValue / thresholdPx).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = if (isDragging) 0 else 120),
        label = "swipeDismissProgress",
    )
    val animatedDistancePx by animateFloatAsState(
        targetValue = visualSignedDistancePx(
            signedRawDistancePx = rawDistancePx,
            thresholdPx = thresholdPx,
            dampingRangePx = dampingRangePx,
        ),
        animationSpec = tween(durationMillis = if (isDragging) 0 else 120),
        label = "swipeDismissDistance",
    )

    LaunchedEffect(visible) {
        if (!visible) {
            delay(SwipeDismissSettingsItemDefaults.ExitAnimationMillis.toLong())
            currentOnDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier.fillMaxWidth(),
        exit = slideOutHorizontally(
            animationSpec = tween(durationMillis = 180),
            targetOffsetX = { (-exitDirection * it / 3).roundToInt() },
        ) + fadeOut(animationSpec = tween(durationMillis = 180)) +
                shrinkVertically(animationSpec = tween(durationMillis = 180)),
    ) {
        val shape = G2CardShape(28.dp)
        val backgroundColor = lerp(
            start = MaterialTheme.colorScheme.surfaceContainer,
            stop = MaterialTheme.colorScheme.error,
            fraction = animatedProgress,
        )
        val contentColor = lerp(
            start = MaterialTheme.colorScheme.onSurface,
            stop = MaterialTheme.colorScheme.onError,
            fraction = animatedProgress,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(backgroundColor, shape)
                .pointerInput(enabled, thresholdPx, visible) {
                    detectDragGestures(
                        onDragStart = {
                            isDragging = true
                        },
                        onDragCancel = {
                            isDragging = false
                            rawDistancePx = 0f
                        },
                        onDragEnd = {
                            isDragging = false
                            val endProgress = (rawDistancePx.absoluteValue / thresholdPx).coerceIn(0f, 1f)
                            if (endProgress >= SwipeDismissSettingsItemDefaults.DeleteProgressThreshold) {
                                exitDirection = if (rawDistancePx >= 0f) 1f else -1f
                                visible = false
                            } else {
                                rawDistancePx = 0f
                            }
                        },
                        onDrag = { change, dragAmount ->
                            if (!enabled || !visible) {
                                return@detectDragGestures
                            }
                            val nextDistance = rawDistancePx - dragAmount.x
                            if (nextDistance != 0f || rawDistancePx != 0f) {
                                change.consume()
                            }
                            rawDistancePx = nextDistance
                        },
                    )
                },
        ) {
            Icon(
                imageVector = dismissIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onError,
                modifier = Modifier
                    .align(if (rawDistancePx < 0f) Alignment.CenterStart else Alignment.CenterEnd)
                    .then(
                        if (rawDistancePx < 0f) {
                            Modifier.padding(start = iconAnchor)
                        } else {
                            Modifier.padding(end = iconAnchor)
                        },
                    )
                    .alpha(animatedProgress)
                    .size(22.dp),
            )
            SettingsItemSurface(
                enabled = enabled,
                onClick = onClick,
                modifier = Modifier
                    .offset { IntOffset(x = -animatedDistancePx.roundToInt(), y = 0) },
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = contentColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

object SwipeDismissSettingsItemDefaults {
    val Threshold: Dp = 40.dp
    val IconAnchor: Dp = 20.dp
    val DampingRange: Dp = 72.dp
    const val DeleteProgressThreshold: Float = 1f
    const val ExitAnimationMillis: Int = 180
}

private fun visualSignedDistancePx(
    signedRawDistancePx: Float,
    thresholdPx: Float,
    dampingRangePx: Float,
): Float {
    val direction = if (signedRawDistancePx >= 0f) 1f else -1f
    val rawDistancePx = signedRawDistancePx.absoluteValue
    val dampedDistancePx = if (rawDistancePx <= thresholdPx) {
        rawDistancePx
    } else {
        thresholdPx + dampingRangePx * (1f - exp(-(rawDistancePx - thresholdPx) / dampingRangePx))
    }
    return direction * dampedDistancePx
}
