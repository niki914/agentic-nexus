package com.niki914.nexus.agentic.app.ui.infra.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * 卡片内的 Toggle 行：与 SettingsNavigationRow 同一套按压体感，
 * 右侧使用 StyledSwitch（基建版，颜色走 MaterialTheme）。
 *
 * 排版与点击行为对齐 ui/settings/StyledToggle 的 independent=false 分支：
 * - 整行点击切换 checked
 * - 按压渐变背景（onSurface 8% 叠加）
 * - 切换时 ContextClick / ToggleOn|Off 触感反馈
 *
 * 颜色全部走 MaterialTheme.colorScheme，深浅色由主题自动适配。
 */
@Composable
fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
) {
    val currentChecked by rememberUpdatedState(checked)
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val restingColor = Color.Transparent
    val pressedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    var backgroundColor by remember { mutableStateOf(restingColor) }
    val animatedBackgroundColor by animateColorAsState(
        targetValue = backgroundColor,
        animationSpec = tween(durationMillis = 500),
        label = "toggleRowBackground",
    )

    Row(
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
                                haptics.performHapticFeedback(
                                    if (!currentChecked) HapticFeedbackType.ToggleOn
                                    else HapticFeedbackType.ToggleOff
                                )
                            }
                            onCheckedChange(!currentChecked)
                        }
                    },
                )
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        LiquidToggle(
            checked = checked,
            enabled = enabled,
            onCheckedChange = { newChecked ->
                if (enabled) onCheckedChange(newChecked)
            },
        )
    }
}
