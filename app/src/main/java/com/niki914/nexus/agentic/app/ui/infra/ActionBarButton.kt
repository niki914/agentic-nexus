package com.niki914.nexus.agentic.app.ui.infra

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

@Composable
internal fun ActionBarButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
    backdrop: LayerBackdrop = rememberLayerBackdrop(),
    content: @Composable () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val progressAnimationSpec = spring(0.5f, 300f, 0.001f)
    val offsetAnimationSpec = spring(1f, 300f, Offset.VisibilityThreshold)
    val progressAnimation = remember { Animatable(0f) }
    val offsetAnimation = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var pressStartPosition by remember { mutableStateOf(Offset.Zero) }
    val innerShadowLayer = rememberGraphicsLayer().apply {
        compositingStrategy = CompositingStrategy.Offscreen
    }
    val density = LocalDensity.current

    val interactiveHighlightShader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            RuntimeShader(
                """
uniform float2 size;
layout(color) uniform half4 color;
uniform float radius;
uniform float2 offset;

half4 main(float2 coord) {
    float2 center = offset;
    float dist = distance(coord, center);
    float intensity = smoothstep(radius, radius * 0.5, dist);
    return color * intensity;
}"""
            )
        } else {
            null
        }
    }
    val isDarkTheme = isSystemInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .requiredSize(with(density) { 48.sp.toDp() })
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(56.dp) },
                highlight = { Highlight.Ambient.copy(alpha = if (isDarkTheme) 1f else 0f) },
                shadow = {
                    Shadow(
                        radius = 12f.dp,
                        color = Color.Black.copy(if (isDarkTheme) 0.08f else 0.2f)
                    )
                },
                layerBlock = {
                    if (!enabled) return@drawBackdrop
                    val width = size.width
                    val height = size.height

                    val progress = progressAnimation.value
                    val scale = lerp(1f, 1.5f, progress)

                    val maxOffset = size.minDimension
                    val initialDerivative = 0.05f
                    val offset = offsetAnimation.value
                    translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                    translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)

                    val maxDragScale = 0.1f
                    val offsetAngle = atan2(offset.y, offset.x)
                    scaleX =
                        scale +
                            maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                            (width / height).fastCoerceAtMost(1f)
                    scaleY =
                        scale +
                            maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                            (height / width).fastCoerceAtMost(1f)
                },
                onDrawSurface = {
                    if (!enabled) {
                        drawRect(
                            (if (isDarkTheme) Color(0xFFAFAFAF) else Color.White).copy(0.5f)
                        )
                        return@drawBackdrop
                    }
                    val progress = progressAnimation.value.coerceIn(0f, 1f)

                    val shape = RoundedCornerShape(56.dp)
                    val outline = shape.createOutline(size, layoutDirection, this)
                    val innerShadowOffset = 4f.dp.toPx()
                    val innerShadowBlurRadius = 4f.dp.toPx()

                    innerShadowLayer.alpha = progress
                    innerShadowLayer.renderEffect =
                        BlurEffect(innerShadowBlurRadius, innerShadowBlurRadius, TileMode.Decal)
                    innerShadowLayer.record {
                        drawOutline(outline, Color.Black.copy(0.2f))
                        translate(0f, innerShadowOffset) {
                            drawOutline(outline, Color.Transparent, blendMode = BlendMode.Clear)
                        }
                    }
                    drawLayer(innerShadowLayer)

                    drawRect(
                        (if (isDarkTheme) Color(0xFFAFAFAF) else Color.White).copy(
                            progress.coerceIn(0.15f, 0.35f)
                        )
                    )
                },
                onDrawFront = {
                    if (!enabled) return@drawBackdrop
                    val progress = progressAnimation.value.fastCoerceIn(0f, 1f)
                    if (progress > 0f) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            interactiveHighlightShader != null
                        ) {
                            drawRect(
                                Color.White.copy(0.1f * progress),
                                blendMode = BlendMode.Plus
                            )
                            interactiveHighlightShader.apply {
                                val offset = pressStartPosition + offsetAnimation.value
                                setFloatUniform("size", size.width, size.height)
                                setColorUniform(
                                    "color",
                                    Color.White.copy(0.15f * progress).toArgb()
                                )
                                setFloatUniform("radius", size.maxDimension)
                                setFloatUniform(
                                    "offset",
                                    offset.x.fastCoerceIn(0f, size.width),
                                    offset.y.fastCoerceIn(0f, size.height)
                                )
                            }
                            drawRect(
                                ShaderBrush(interactiveHighlightShader),
                                blendMode = BlendMode.Plus
                            )
                        } else {
                            drawRect(
                                Color.White.copy(0.25f * progress),
                                blendMode = BlendMode.Plus
                            )
                        }
                    }
                },
                effects = {
                    lens(
                        refractionHeight = 6f.dp.toPx(),
                        refractionAmount = size.height / 2f,
                        depthEffect = true,
                        chromaticAberration = true
                    )
                },
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
            ) {
                scope.launch { haptics.performHapticFeedback(HapticFeedbackType.ContextClick) }
                onClick()
            }
            .pointerInput(scope) {
                val onDragStop: () -> Unit = {
                    if (enabled) {
                        scope.launch {
                            launch { haptics.performHapticFeedback(HapticFeedbackType.Reject) }
                            launch { progressAnimation.animateTo(0f, progressAnimationSpec) }
                            launch { offsetAnimation.animateTo(Offset.Zero, offsetAnimationSpec) }
                        }
                    }
                }
                inspectDragGestures(
                    onDragStart = { down ->
                        if (enabled) {
                            pressStartPosition = down.position
                            scope.launch {
                                launch {
                                    haptics.performHapticFeedback(
                                        HapticFeedbackType.SegmentFrequentTick
                                    )
                                }
                                launch { progressAnimation.animateTo(1f, progressAnimationSpec) }
                                launch { offsetAnimation.snapTo(Offset.Zero) }
                            }
                        }
                    },
                    onDragEnd = { onDragStop() },
                    onDragCancel = onDragStop
                ) { _, dragAmount ->
                    scope.launch {
                        if (enabled) {
                            if (dragAmount.getDistanceSquared() > 350) {
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                            }
                            offsetAnimation.snapTo(offsetAnimation.value + dragAmount)
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

// --- Drag gesture inspector, copied from librepods DragGestureInspector ---

private suspend fun PointerInputScope.inspectDragGestures(
    onDragStart: (down: PointerInputChange) -> Unit = {},
    onDragEnd: (change: PointerInputChange) -> Unit = {},
    onDragCancel: () -> Unit = {},
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit
) {
    awaitEachGesture {
        val initialDown = awaitFirstDown(false, PointerEventPass.Initial)
        val down = awaitFirstDown(false)

        onDragStart(down)
        onDrag(initialDown, Offset.Zero)
        val upEvent =
            drag(pointerId = initialDown.id, onDrag = { onDrag(it, it.positionChange()) })
        if (upEvent == null) {
            onDragCancel()
        } else {
            onDragEnd(upEvent)
        }
    }
}

private suspend inline fun AwaitPointerEventScope.drag(
    pointerId: PointerId,
    onDrag: (PointerInputChange) -> Unit
): PointerInputChange? {
    val isPointerUp =
        currentEvent.changes.fastFirstOrNull { it.id == pointerId }?.pressed != true
    if (isPointerUp) {
        return null
    }
    var pointer = pointerId
    while (true) {
        val change = awaitDragOrUp(pointer) ?: return null
        if (change.isConsumed) {
            return null
        }
        if (change.changedToUpIgnoreConsumed()) {
            return change
        }
        onDrag(change)
        pointer = change.id
    }
}

private suspend inline fun AwaitPointerEventScope.awaitDragOrUp(
    pointerId: PointerId
): PointerInputChange? {
    var pointer = pointerId
    while (true) {
        val event = awaitPointerEvent()
        val dragEvent = event.changes.fastFirstOrNull { it.id == pointer } ?: return null
        if (dragEvent.changedToUpIgnoreConsumed()) {
            val otherDown = event.changes.fastFirstOrNull { it.pressed }
            if (otherDown == null) {
                return dragEvent
            } else {
                pointer = otherDown.id
            }
        } else {
            val hasDragged = dragEvent.previousPosition != dragEvent.position
            if (hasDragged) {
                return dragEvent
            }
        }
    }
}
