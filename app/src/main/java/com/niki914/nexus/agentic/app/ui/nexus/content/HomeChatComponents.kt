package com.niki914.nexus.agentic.app.ui.nexus.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.continuities.G2Continuity
import com.niki914.nexus.agentic.app.R
import com.niki914.nexus.agentic.app.ui.infra.component.G2RoundedCornerShape
import com.niki914.nexus.agentic.app.ui.infra.component.LiquidTextField
import com.niki914.nexus.agentic.app.ui.nexus.model.HomeToolState
import com.niki914.nexus.agentic.app.ui.nexus.model.HomeToolStatus

@Composable
fun AssistantOutputText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyLarge.copy(
            fontSize = (MaterialTheme.typography.bodyLarge.fontSize.value + 1f).sp,
            lineHeight = (MaterialTheme.typography.bodyLarge.lineHeight.value + 2f).sp,
        ),
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
fun UserMessageBubble(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val bubbleShape = G2RoundedCornerShape(24.dp)

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = maxWidth * 0.82f)
                .clip(bubbleShape)
                .background(colorScheme.primary.copy(alpha = 0.18f), bubbleShape)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = colorScheme.onSurface,
                textAlign = TextAlign.Start,
            )
        }
    }
}

@Composable
fun ToolStatusPill(
    status: HomeToolStatus,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val containerColor = colorScheme.surfaceVariant.copy(alpha = 0.72f)
    val contentColor = colorScheme.onSurfaceVariant.copy(alpha = 0.82f)
    val shape = ContinuousCapsule(G2Continuity())

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .clip(shape)
                .background(containerColor, shape)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            ToolStatusIndicator(
                state = status.state,
                color = contentColor,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = status.name,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = status.state.label(),
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
            )
        }
    }
}

@Composable
fun LiquidChatComposer(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    sendEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val canSend = sendEnabled && value.isNotBlank()
    val contentColor = if (canSend) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    }

    LiquidTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = stringResource(R.string.nexus_home_input_placeholder),
        enabled = true,
        singleLine = false,
        modifier = modifier.fillMaxWidth(),
        trailingContent = {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                IconButton(
                    onClick = onSendClick,
                    enabled = canSend,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = stringResource(R.string.nexus_home_send_content_description),
                    )
                }
            }
        },
    )
}

@Composable
private fun ToolStatusIndicator(
    state: HomeToolState,
    color: Color,
) {
    when (state) {
        HomeToolState.Running -> CircularProgressIndicator(
            modifier = Modifier.size(14.dp),
            color = color,
            strokeWidth = 2.dp,
        )
        HomeToolState.Succeeded,
        HomeToolState.Failed,
        -> Box(
            modifier = Modifier
                .size(10.dp)
                .clip(ContinuousCapsule(G2Continuity()))
                .background(color.copy(alpha = 0.78f)),
        )
    }
}

@Composable
private fun HomeToolState.label(): String = when (this) {
    HomeToolState.Running -> stringResource(R.string.nexus_home_tool_running)
    HomeToolState.Succeeded -> stringResource(R.string.nexus_home_tool_succeeded)
    HomeToolState.Failed -> stringResource(R.string.nexus_home_tool_failed)
}
