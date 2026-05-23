package com.niki914.nexus.agentic.app.ui.nexus.content

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.niki914.nexus.agentic.app.ui.nexus.model.HomeChatController
import com.niki914.nexus.agentic.app.ui.nexus.model.HomeChatTurn
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@Composable
fun HomePageContent(
    topPadding: Dp,
    hazeState: HazeState,
    chatController: HomeChatController,
) {
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .hazeSource(hazeState),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = topPadding + 24.dp,
                end = 20.dp,
                bottom = 128.dp,
            ),
        ) {
            items(
                items = chatController.turns,
                key = { turn -> turn.id },
            ) { turn ->
                HomeChatTurnItem(
                    turn = turn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 18.dp),
                )
            }
        }

        LiquidChatComposer(
            value = chatController.input,
            onValueChange = chatController::onInputChange,
            onSendClick = { chatController.send(scope) },
            enabled = !chatController.isGenerating,
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 20.dp),
        )
    }
}

@Composable
private fun HomeChatTurnItem(
    turn: HomeChatTurn,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        UserMessageBubble(text = turn.userText)

        turn.toolStatus?.let { status ->
            ToolStatusPill(
                status = status,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        if (turn.assistantText.isNotBlank()) {
            AssistantOutputText(
                text = turn.assistantText,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        turn.errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
            Text(
                text = message,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
