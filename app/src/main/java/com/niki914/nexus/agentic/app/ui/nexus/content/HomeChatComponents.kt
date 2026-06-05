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
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.rememberMarkdownState
import com.niki914.nexus.agentic.app.R
import com.niki914.nexus.agentic.app.ui.infra.component.LiquidTextField
import com.niki914.nexus.agentic.app.ui.infra.shape.G2BubbleShape
import com.niki914.nexus.agentic.app.ui.infra.shape.G2CapsuleShape
import com.niki914.nexus.agentic.app.ui.nexus.model.HomeToolState
import com.niki914.nexus.agentic.app.ui.nexus.model.HomeToolStatus
import com.niki914.nexus.cb.BaseTheme

private val ToolSucceededIndicatorColor = Color(0xFF4F8F6B)
private val ToolFailedIndicatorColor = Color(0xFFB85C5C)
internal data class AssistantErrorUi(
    val title: String,
    val body: String,
)

internal fun toAssistantErrorUi(message: String): AssistantErrorUi {
    return when (message.trim()) {
        "请先填写配置" -> AssistantErrorUi(
            title = "配置还没填好",
            body = "请先填写模型地址和模型名称。",
        )

        "" -> AssistantErrorUi(
            title = "当前无法继续",
            body = "请稍后重试。",
        )

        else -> AssistantErrorUi(
            title = "当前无法继续",
            body = message.trim(),
        )
    }
}

private const val AssistantMarkdownPreviewText = """
# Nexus 对话排版

这是一段用于观察正文、标题、引用和表格体感的示例内容。标题不应该再像页面 Hero 一样夸张。

## 标题层级

- 一级信息要明显
- 二级信息要克制
- 列表和正文尽量共用节奏

### 表格密度

| 项目 | 目标 |
| --- | --- |
| H1 | 明显但不撑爆聊天流 |
| H2 | 比正文大一档 |
| Table | 与正文接近，便于连续阅读 |

> 这是一段引用文字，用来确认弱化后的信息层级是否还清楚。

`inline code`

```kotlin
val answer = "markdown preview"
println(answer)
```
"""

@Composable
fun AssistantOutputText(
    text: String,
    modifier: Modifier = Modifier,
) {
    val bodyStyle = MaterialTheme.typography.bodyLarge.copy(
        fontSize = (MaterialTheme.typography.bodyLarge.fontSize.value + 1f).sp,
        lineHeight = (MaterialTheme.typography.bodyLarge.lineHeight.value + 2f).sp,
    )
    val h1Style = bodyStyle.copy(
        fontSize = 18.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
    )
    val h2Style = bodyStyle.copy(
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
    )
    val h3Style = bodyStyle.copy(
        fontSize = 14.sp,
        lineHeight = 19.sp,
        fontWeight = FontWeight.SemiBold,
    )
    val h4Style = bodyStyle.copy(
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Medium,
    )
    val h5Style = bodyStyle.copy(
        fontSize = 12.sp,
        lineHeight = 17.sp,
        fontWeight = FontWeight.Medium,
    )
    val h6Style = bodyStyle.copy(
        fontSize = 11.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
    )
    val tableStyle = bodyStyle.copy(
        fontSize = 16.sp,
        lineHeight = 22.sp,
    )
    val codeStyle = bodyStyle.copy(
        fontSize = 15.sp,
        lineHeight = 22.sp,
        fontFamily = FontFamily.Monospace,
    )
    val quoteStyle = bodyStyle.copy(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontStyle = FontStyle.Italic,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val markdownState = rememberMarkdownState(
        content = text,
        immediate = true,
    )

    Markdown(
        markdownState = markdownState,
        modifier = modifier.fillMaxWidth(),
        typography = markdownTypography(
            h1 = h1Style,
            h2 = h2Style,
            h3 = h3Style,
            h4 = h4Style,
            h5 = h5Style,
            h6 = h6Style,
            text = bodyStyle,
            code = codeStyle,
            inlineCode = codeStyle,
            quote = quoteStyle,
            paragraph = bodyStyle,
            ordered = bodyStyle,
            bullet = bodyStyle,
            list = bodyStyle,
            table = tableStyle,
        ),
    )
}

@Preview(
    name = "Assistant Markdown Preview",
    showBackground = true,
    widthDp = 420,
)
@Composable
private fun AssistantOutputTextPreview() {
    BaseTheme {
        AssistantOutputText(
            text = AssistantMarkdownPreviewText.trim(),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
fun UserMessageBubble(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val bubbleShape = G2BubbleShape(24.dp)

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
    val shape = G2CapsuleShape()

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            val maxNameWidth = maxWidth * 0.56f

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
                    modifier = Modifier.widthIn(max = maxNameWidth),
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = status.state.label(),
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

@Composable
fun AssistantErrorBlock(
    message: String,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val shape = G2BubbleShape(18.dp)
    val errorUi = toAssistantErrorUi(message)

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = maxWidth * 0.82f)
                .clip(shape)
                .background(colorScheme.errorContainer.copy(alpha = 0.68f), shape)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = errorUi.title,
                style = MaterialTheme.typography.labelLarge,
                color = colorScheme.onErrorContainer,
                textAlign = TextAlign.Center,
            )
            Text(
                text = errorUi.body,
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onErrorContainer,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun LiquidChatComposer(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onStopClick: () -> Unit,
    isGenerating: Boolean,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
) {
    val canSend = !isGenerating && value.isNotBlank()
    val buttonEnabled = isGenerating || canSend
    val contentColor = if (buttonEnabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    }

    LiquidTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = stringResource(R.string.ui_home_input_placeholder),
        enabled = true,
        singleLine = false,
        maxLines = maxLines,
        modifier = modifier.fillMaxWidth(),
        trailingContent = {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                IconButton(
                    onClick = if (isGenerating) onStopClick else onSendClick,
                    enabled = buttonEnabled,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = if (isGenerating) Icons.Default.Stop else Icons.Default.Send,
                        contentDescription = stringResource(
                            if (isGenerating) {
                                R.string.ui_home_stop_content_description
                            } else {
                                R.string.ui_home_send_content_description
                            }
                        ),
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
            -> {
            val indicatorColor = when (state) {
                HomeToolState.Succeeded -> ToolSucceededIndicatorColor
                HomeToolState.Failed -> ToolFailedIndicatorColor
                HomeToolState.Running -> color
            }
            val indicatorShape = G2CapsuleShape()
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(indicatorShape)
                    .background(indicatorColor.copy(alpha = 0.82f), indicatorShape),
            )
        }
    }
}

@Composable
private fun HomeToolState.label(): String = when (this) {
    HomeToolState.Running -> stringResource(R.string.ui_tool_status_running)
    HomeToolState.Succeeded -> stringResource(R.string.ui_tool_status_success)
    HomeToolState.Failed -> stringResource(R.string.ui_tool_status_failed)
}
