package com.niki914.nexus.agentic.app.ui.infra

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.niki914.nexus.agentic.app.ui.infra.shape.G2FieldShape

@Composable
fun LiquidDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    dismissOnBackgroundTap: Boolean = true,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val backdrop = rememberLayerBackdrop()
    val colorScheme = MaterialTheme.colorScheme
    val scrimColor = colorScheme.scrim.copy(alpha = 0.42f)
    val panelSurfaceColor = colorScheme.surfaceContainerHigh.copy(alpha = 0.76f)
    val panelTintColor = colorScheme.primaryContainer.copy(alpha = 0.18f)
    val interactionSource = remember { MutableInteractionSource() }
    val panelShape = G2FieldShape(48.dp)

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(240, easing = FastOutSlowInEasing)) +
                scaleIn(
                    initialScale = 1.04f,
                    animationSpec = tween(240, easing = FastOutSlowInEasing),
                ),
        exit = fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                scaleOut(
                    targetScale = 1.02f,
                    animationSpec = tween(180, easing = FastOutSlowInEasing),
                ),
    ) {
        Box(
            modifier = modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scrimColor)
                    .clickable(
                        enabled = dismissOnBackgroundTap,
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onDismissRequest,
                    ),
            )

            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .requiredWidthIn(min = 200.dp, max = 360.dp)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { panelShape },
                        effects = {
                            vibrancy()
                            blur(4.dp.toPx())
                            lens(14.dp.toPx(), 28.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(panelTintColor, blendMode = BlendMode.Hue)
                            drawRect(panelSurfaceColor)
                        },
                    )
                    .clip(panelShape)
                    .clickable(
                        enabled = false,
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {},
                    )
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                title?.invoke()
                text?.invoke()
                content?.invoke(this)
                if (actions != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        content = actions,
                    )
                }
            }
        }
    }
}
