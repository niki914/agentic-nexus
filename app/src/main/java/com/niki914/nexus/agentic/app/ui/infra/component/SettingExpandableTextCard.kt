package com.niki914.nexus.agentic.app.ui.infra.component

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.niki914.nexus.agentic.app.ui.infra.shape.G2CardShape

@Composable
fun SettingExpandableTextCard(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
    initiallyExpanded: Boolean = false,
    expanded: Boolean? = null,
    minLines: Int = 4,
    maxLines: Int = 10,
    onExpandedChange: ((Boolean) -> Unit)? = null,
) {
    var internalExpanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    val isExpanded = expanded ?: internalExpanded
    val focusRequester = remember { FocusRequester() }

    fun updateExpanded(value: Boolean) {
        if (expanded == null) {
            internalExpanded = value
        }
        onExpandedChange?.invoke(value)
    }

    LaunchedEffect(isExpanded) {
        if (isExpanded && enabled) {
            focusRequester.requestFocus()
        }
    }

    SettingExpandableTextCardContent(
        title = title,
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        description = description,
        expanded = isExpanded,
        enabled = enabled,
        minLines = minLines,
        maxLines = maxLines,
        focusRequester = focusRequester,
        onToggleExpanded = {
            if (enabled) {
                updateExpanded(!isExpanded)
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun SettingExpandableTextCardContent(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    description: String?,
    expanded: Boolean,
    enabled: Boolean,
    minLines: Int,
    maxLines: Int,
    focusRequester: FocusRequester,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val animationSpec = tween<IntSize>(durationMillis = 320, easing = FastOutSlowInEasing)
    val cardShape = G2CardShape(28.dp)
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "settingExpandableTextCardArrowRotation",
    )
    val colorScheme = MaterialTheme.colorScheme
    val cardColor = if (enabled) {
        colorScheme.surfaceContainer
    } else {
        colorScheme.surfaceContainer.copy(alpha = 0.58f)
    }
    val titleColor = if (enabled) {
        colorScheme.onSurface
    } else {
        colorScheme.onSurface.copy(alpha = 0.45f)
    }
    val secondaryTextColor = if (enabled) {
        colorScheme.onSurfaceVariant
    } else {
        colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(cardColor, cardShape)
            .animateContentSize(animationSpec = animationSpec)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (enabled) {
                        Modifier.pointerInput(expanded) {
                            detectTapGestures(onTap = { onToggleExpanded() })
                        }
                    } else {
                        Modifier
                    }
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = secondaryTextColor,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        rotationZ = arrowRotation
                    },
            )
        }

        if (description != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = secondaryTextColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(12.dp))
            LiquidTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = placeholder,
                enabled = enabled,
                minLines = minLines,
                maxLines = maxLines,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 136.dp)
                    .focusRequester(focusRequester),
            )
        }
    }
}
