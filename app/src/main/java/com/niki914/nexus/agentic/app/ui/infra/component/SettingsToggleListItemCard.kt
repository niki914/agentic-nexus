package com.niki914.nexus.agentic.app.ui.infra.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
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

@Composable
fun SettingsToggleListItemCard(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    SettingsGroupCard(
        title = null,
        modifier = modifier,
    ) {
        SettingsToggleListItemRow(
            title = title,
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            onClick = onClick,
        )
    }
}

@Composable
private fun SettingsToggleListItemRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean,
    onClick: (() -> Unit)?,
) {
    val currentChecked by rememberUpdatedState(checked)
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val restingColor = Color.Transparent
    val pressedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    var backgroundColor by remember { mutableStateOf(restingColor) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .pointerInput(enabled, onClick) {
                    detectTapGestures(
                        onPress = {
                            if (enabled && onClick != null) {
                                backgroundColor = pressedColor
                                tryAwaitRelease()
                                backgroundColor = restingColor
                            }
                        },
                        onTap = {
                            if (enabled && onClick != null) {
                                scope.launch {
                                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                }
                                onClick()
                            }
                        },
                    )
                },
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        LiquidToggle(
            checked = checked,
            enabled = enabled,
            onCheckedChange = { newChecked ->
                if (enabled && newChecked != currentChecked) {
                    onCheckedChange(newChecked)
                }
            },
        )
    }
}
