package com.niki914.nexus.agentic.app.ui.nexus.content

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.niki914.nexus.agentic.app.ui.infra.nav.pageViewModel
import com.niki914.nexus.agentic.app.ui.nexus.model.HomeChatBlock
import com.niki914.nexus.agentic.app.ui.nexus.model.HomeChatIntent
import com.niki914.nexus.agentic.app.ui.nexus.model.HomeChatTurn
import com.niki914.nexus.agentic.app.ui.nexus.model.HomeChatViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@Composable
fun HomePageContent(
    topPadding: Dp,
    hazeState: HazeState,
) {
    val viewModel = pageViewModel<HomeChatViewModel>()
    val uiState by viewModel.uiStateFlow.collectAsState()
    val density = LocalDensity.current
    val imeBottom = with(density) { WindowInsets.ime.getBottom(this).toDp() }
    val navigationBottom = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
    val composerBottomPadding = (imeBottom + 12.dp).coerceAtLeast(navigationBottom + 20.dp)

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
                items = uiState.turns,
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

        LiquidChatComposer( // TODO P2 限制输入框最大行数
            value = uiState.input,
            onValueChange = { value ->
                viewModel.sendIntent(HomeChatIntent.InputChanged(value))
            },
            onSendClick = {
                viewModel.sendIntent(HomeChatIntent.Send)
            },
            sendEnabled = !uiState.isGenerating,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = 20.dp,
                    end = 20.dp,
                    bottom = composerBottomPadding,
                ),
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

        turn.blocks.forEach { block ->
            when (block) {
                is HomeChatBlock.Text -> {
                    if (block.text.isNotBlank()) {
                        AssistantOutputText(
                            text = block.text,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
                is HomeChatBlock.Tool -> {
                    ToolStatusPill(
                        status = block.status,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
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
