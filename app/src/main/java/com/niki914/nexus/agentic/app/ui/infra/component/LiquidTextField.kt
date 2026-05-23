package com.niki914.nexus.agentic.app.ui.infra.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.continuities.G2Continuity
import com.niki914.nexus.agentic.app.liquid_example.utils.InteractiveHighlight
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

@Composable
fun LiquidTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = false,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
) {
    val backdrop = rememberLayerBackdrop()
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }

    val colorScheme = MaterialTheme.colorScheme
    val textColor = if (enabled) {
        colorScheme.onSurface
    } else {
        colorScheme.onSurface.copy(alpha = 0.45f)
    }
    val placeholderColor = if (enabled) {
        colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
    } else {
        colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    }
    val trailingColor = if (enabled) {
        colorScheme.onSurfaceVariant
    } else {
        colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    }
    val surfaceColor = if (enabled) {
        colorScheme.surface.copy(alpha = 0.18f)
    } else {
        colorScheme.surfaceVariant.copy(alpha = 0.16f)
    }
    val tintColor = if (enabled) {
        colorScheme.primary.copy(alpha = 0.08f)
    } else {
        Color.Transparent
    }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { ContinuousCapsule(G2Continuity()) },
                effects = {
                    vibrancy()
                    blur(2.dp.toPx())
                    lens(12.dp.toPx(), 24.dp.toPx())
                },
                layerBlock = if (enabled) {
                    {
                        if (size.width == 0f || size.height == 0f) return@drawBackdrop

                        val width = size.width
                        val height = size.height
                        val progress = interactiveHighlight.pressProgress
                        val scale = lerp(1f, 1f + 3.dp.toPx() / height, progress)

                        val maxOffset = size.minDimension
                        val initialDerivative = 0.05f
                        val offset = interactiveHighlight.offset
                        translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                        translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)

                        val maxDragScale = 3.dp.toPx() / height
                        val offsetAngle = atan2(offset.y, offset.x)
                        scaleX =
                            scale +
                                maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                                (width / height).fastCoerceAtMost(1f)
                        scaleY =
                            scale +
                                maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                                (height / width).fastCoerceAtMost(1f)
                    }
                } else {
                    null
                },
                onDrawSurface = {
                    if (enabled) {
                        drawRect(tintColor, blendMode = BlendMode.Hue)
                    }
                    drawRect(surfaceColor)
                },
            )
            .clip(ContinuousCapsule(G2Continuity()))
            .then(if (enabled) interactiveHighlight.modifier else Modifier)
            .then(if (enabled) interactiveHighlight.gestureModifier else Modifier)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        enabled = enabled,
        singleLine = singleLine,
        textStyle = MaterialTheme.typography.bodyLarge.merge(
            TextStyle(color = textColor),
        ),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(min = 0.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isBlank()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = placeholderColor,
                        )
                    }
                    innerTextField()
                }

                if (trailingContent != null) {
                    CompositionLocalProvider(LocalContentColor provides trailingColor) {
                        trailingContent()
                    }
                }
            }
        },
    )
}
