package com.niki914.nexus.agentic.app.ui.infra.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * 组内导航子项：保持透明 item 语义，由外层 `SettingsGroupCard` 负责圆角与底色。
 */
@Composable
fun SettingNavigationItem(
    title: String,
    summary: String,
    modifier: Modifier = Modifier,
    currentState: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val restingColor = Color.Transparent
    val pressedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    var backgroundColor by remember { mutableStateOf(restingColor) }
    val animatedBackgroundColor by animateColorAsState(
        targetValue = backgroundColor,
        animationSpec = tween(durationMillis = 500),
        label = "settingNavigationItemBackground",
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(animatedBackgroundColor)
            .heightIn(min = 64.dp)
            .pointerInput(enabled) {
                detectTapGestures(
                    onPress = {
                        if (enabled) {
                            backgroundColor = pressedColor
                            tryAwaitRelease()
                            backgroundColor = restingColor
                        }
                    },
                    onTap = {
                        if (enabled) {
                            scope.launch {
                                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                            }
                            onClick()
                        }
                    },
                )
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        val titleAreaMaxWidth = maxWidth * 0.66f
        val stateAreaMaxWidth = maxWidth * 0.22f

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(
                modifier = Modifier.widthIn(max = titleAreaMaxWidth),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (currentState != null) {
                Text(
                    text = currentState,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = stateAreaMaxWidth),
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
