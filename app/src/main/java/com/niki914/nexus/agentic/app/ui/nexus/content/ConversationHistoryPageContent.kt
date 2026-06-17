package com.niki914.nexus.agentic.app.ui.nexus.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.niki914.nexus.agentic.app.R
import com.niki914.nexus.agentic.app.conversation.ConversationSummary
import com.niki914.nexus.agentic.app.ui.infra.ProvideLiquidScreenContentForPreview
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsGroupCard
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsListItem
import com.niki914.nexus.agentic.app.ui.infra.liquidScreenHazeSource
import com.niki914.nexus.agentic.app.ui.infra.liquidScreenTopPadding
import com.niki914.nexus.cb.BaseTheme

internal data class ConversationHistoryUiState(
    val isLoading: Boolean = false,
    val conversations: List<ConversationSummary> = emptyList(),
    val errorMessage: String? = null,
)

@Composable
internal fun ConversationHistoryPageContent(
    uiState: ConversationHistoryUiState,
    onConversationClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isLoading -> ConversationHistoryMessageContent(
            title = stringResource(R.string.ui_conversation_history_loading),
            modifier = modifier,
        )

        uiState.errorMessage != null -> ConversationHistoryMessageContent(
            title = stringResource(R.string.ui_conversation_history_error_title),
            body = uiState.errorMessage,
            modifier = modifier,
        )

        uiState.conversations.isEmpty() -> ConversationHistoryMessageContent(
            title = stringResource(R.string.ui_conversation_history_empty_title),
            body = stringResource(R.string.ui_conversation_history_empty_body),
            modifier = modifier,
        )

        else -> ConversationHistoryListContent(
            conversations = uiState.conversations,
            onConversationClick = onConversationClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun ConversationHistoryListContent(
    conversations: List<ConversationSummary>,
    onConversationClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val untitledConversation = stringResource(R.string.ui_conversation_history_untitled)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .liquidScreenHazeSource(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = liquidScreenTopPadding(24.dp),
            end = 20.dp,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            items = conversations,
            key = { conversation -> conversation.id },
        ) { conversation ->
            SettingsGroupCard(modifier = Modifier.fillMaxWidth()) {
                SettingsListItem(
                    title = conversation.title.ifBlank { untitledConversation },
                    summary = conversation.lastMessagePreview.takeIf { it.isNotBlank() },
                    showChevron = true,
                    onClick = {
                        onConversationClick(conversation.id)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ConversationHistoryMessageContent(
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .liquidScreenHazeSource()
            .padding(horizontal = 20.dp)
            .padding(top = liquidScreenTopPadding(24.dp), bottom = 24.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        SettingsGroupCard {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                if (!body.isNullOrBlank()) {
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Preview(
    name = "Conversation History List",
    showBackground = true,
    widthDp = 420,
    heightDp = 900,
)
@Composable
private fun ConversationHistoryListPreview() {
    BaseTheme {
        ProvideLiquidScreenContentForPreview(topPadding = 0.dp) {
            ConversationHistoryPageContent(
                uiState = ConversationHistoryUiState(
                    conversations = listOf(
                        previewConversationSummary(
                            id = "conversation-1",
                            title = "检查当前工具状态",
                            preview = "I've done the check and summarized the result.",
                        ),
                        previewConversationSummary(
                            id = "conversation-2",
                            title = "分析日志",
                            preview = "The failure path starts after the second request.",
                        ),
                    ),
                ),
                onConversationClick = {},
            )
        }
    }
}

@Preview(
    name = "Conversation History Empty",
    showBackground = true,
    widthDp = 420,
    heightDp = 900,
)
@Composable
private fun ConversationHistoryEmptyPreview() {
    BaseTheme {
        ProvideLiquidScreenContentForPreview(topPadding = 0.dp) {
            ConversationHistoryPageContent(
                uiState = ConversationHistoryUiState(),
                onConversationClick = {},
            )
        }
    }
}

@Preview(
    name = "Conversation History Error",
    showBackground = true,
    widthDp = 420,
    heightDp = 900,
)
@Composable
private fun ConversationHistoryErrorPreview() {
    BaseTheme {
        ProvideLiquidScreenContentForPreview(topPadding = 0.dp) {
            ConversationHistoryPageContent(
                uiState = ConversationHistoryUiState(errorMessage = "Database is not ready."),
                onConversationClick = {},
            )
        }
    }
}

private fun previewConversationSummary(
    id: String,
    title: String,
    preview: String,
): ConversationSummary {
    return ConversationSummary(
        id = id,
        title = title,
        titleEdited = false,
        createdAt = 0L,
        updatedAt = 0L,
        lastMessagePreview = preview,
        turnCount = 1,
    )
}
