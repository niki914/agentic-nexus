package com.niki914.nexus.agentic.app.ui.nexus.content

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.niki914.nexus.agentic.app.R
import com.niki914.nexus.agentic.app.ui.infra.LocalLiquidViewportAvoidanceController
import com.niki914.nexus.agentic.app.ui.infra.nav.pageViewModel
import com.niki914.nexus.agentic.app.ui.nexus.PageChromeContribution
import com.niki914.nexus.agentic.app.ui.nexus.PageChromeMenuItem
import com.niki914.nexus.agentic.app.ui.nexus.RegisterPageChrome
import com.niki914.nexus.agentic.app.ui.nexus.model.HomeChatBlock
import com.niki914.nexus.agentic.app.ui.nexus.model.HomeChatIntent
import com.niki914.nexus.agentic.app.ui.nexus.model.HomeChatTurn
import com.niki914.nexus.agentic.app.ui.nexus.model.HomeChatViewModel
import com.niki914.nexus.agentic.app.ui.nexus.model.HomeToolState
import com.niki914.nexus.agentic.app.ui.nexus.model.HomeToolStatus
import com.niki914.nexus.cb.BaseTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.flow.collectLatest

@Composable
fun HomePageContent(
    topPadding: Dp, // TODO 这种透传代码特别多，看看 topPadding 和 hazeState 能不能用 Local 提供
    hazeState: HazeState,
    onOpenSettings: () -> Unit,
) {
    val viewModel = pageViewModel<HomeChatViewModel>()
    val clearMenuLabel = stringResource(R.string.ui_home_menu_clear)
    val settingsMenuLabel = stringResource(R.string.ui_settings_menu_entry)
    val latestViewModel by rememberUpdatedState(viewModel)
    val latestOnOpenSettings by rememberUpdatedState(onOpenSettings)
    val uiState by viewModel.uiStateFlow.collectAsState()
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    val imeBottom = with(density) { WindowInsets.ime.getBottom(this).toDp() }
    val navigationBottom = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
    var isComposerFocused by remember { mutableStateOf(false) }
    val effectiveImeBottom = if (isComposerFocused) imeBottom else 0.dp
    val composerBottomPadding = (effectiveImeBottom + 12.dp).coerceAtLeast(navigationBottom + 20.dp)
    val bottomThresholdPx = with(density) { 24.dp.roundToPx() }
    val isUserDragging by listState.interactionSource.collectIsDraggedAsState()
    var shouldFollowBottom by remember { mutableStateOf(true) }
    var hasPendingUserScrollDecision by remember { mutableStateOf(false) }
    val lastTurn = uiState.turns.lastOrNull()
    val bottomContentVersion = remember(
        uiState.turns.size,
        uiState.streamEventCount,
        lastTurn?.id,
        lastTurn?.blocks?.size,
    ) {
        listOf(
            uiState.turns.size,
            uiState.streamEventCount,
            lastTurn?.id,
            lastTurn?.blocks?.size,
        )
    }
    val isAtBottom by remember(listState, bottomThresholdPx) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisibleItem =
                layoutInfo.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf true
            val viewportEnd = layoutInfo.viewportEndOffset
            lastVisibleItem.index == layoutInfo.totalItemsCount - 1 &&
                    lastVisibleItem.offset + lastVisibleItem.size <= viewportEnd + bottomThresholdPx
        }
    }
    val dismissInputFocus = remember(focusManager, keyboardController) {
        {
            keyboardController?.hide()
            focusManager.clearFocus(force = true)
        }
    }

    LaunchedEffect(isUserDragging) {
        if (isUserDragging) {
            hasPendingUserScrollDecision = true
        }
    }
    LaunchedEffect(listState, isAtBottom) {
        snapshotFlow { listState.isScrollInProgress }
            .collectLatest { isScrollInProgress ->
                if (!isScrollInProgress && hasPendingUserScrollDecision) {
                    shouldFollowBottom = isAtBottom
                    hasPendingUserScrollDecision = false
                }
            }
    }
    LaunchedEffect(bottomContentVersion, shouldFollowBottom) {
        if (shouldFollowBottom) {
            listState.scrollToItem(uiState.turns.size)
        }
    }

    val pageChromeContribution = remember(clearMenuLabel, settingsMenuLabel) {
        PageChromeContribution(
            menuItems = listOf(
                PageChromeMenuItem(
                    key = "clear",
                    title = clearMenuLabel,
                    onClick = {
                        latestViewModel.sendIntent(HomeChatIntent.ClearConversation)
                    },
                ),
                PageChromeMenuItem(
                    key = "settings",
                    title = settingsMenuLabel,
                    onClick = { latestOnOpenSettings() },
                ),
            ),
        )
    }
    RegisterPageChrome(pageChromeContribution)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .hazeSource(hazeState),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = dismissInputFocus,
                ),
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
                    onContentTap = dismissInputFocus,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 18.dp),
                )
            }
            item(key = "bottom_anchor") {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp),
                )
            }
        }

        CompositionLocalProvider(LocalLiquidViewportAvoidanceController provides null) {
            LiquidChatComposer(
                value = uiState.input,
                onValueChange = { value ->
                    viewModel.sendIntent(HomeChatIntent.InputChanged(value))
                },
                onSendClick = {
                    shouldFollowBottom = true
                    viewModel.sendIntent(HomeChatIntent.Send)
                },
                onStopClick = {
                    viewModel.sendIntent(HomeChatIntent.StopGenerating)
                },
                isGenerating = uiState.isGenerating,
                maxLines = 10,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onFocusChanged { focusState ->
                        isComposerFocused = focusState.hasFocus
                    }
                    .padding(
                        start = 20.dp,
                        end = 20.dp,
                        bottom = composerBottomPadding,
                    ),
            )
        }
    }
}

@Composable
private fun HomeChatTurnItem(
    turn: HomeChatTurn,
    onContentTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onContentTap,
        ),
    ) {
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

                is HomeChatBlock.Error -> {
                    AssistantErrorBlock(
                        message = block.message,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }

    }
}

@Preview(
    name = "Home Page Preview",
    showBackground = true,
    widthDp = 420,
    heightDp = 900,
)
@Composable
private fun HomePageContentPreview() {
    BaseTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
        ) {
            HomeChatTurnItem(
                turn = HomeChatTurn(
                    id = 0L,
                    userText = "帮我检查一下当前工具状态。",
                    blocks = listOf(
                        HomeChatBlock.Text("I'll call the available tools first."),
                        HomeChatBlock.Tool(
                            HomeToolStatus(
                                callId = "tool-1",
                                name = "read_session",
                                state = HomeToolState.Succeeded,
                            )
                        ),
                        HomeChatBlock.Tool(
                            HomeToolStatus(
                                callId = "tool-2",
                                name = "update_config",
                                state = HomeToolState.Running,
                            )
                        ),
                        HomeChatBlock.Tool(
                            HomeToolStatus(
                                callId = "tool-3",
                                name = "sync_mcp",
                                state = HomeToolState.Failed,
                            )
                        ),
                        HomeChatBlock.Error("MCP 工具调用失败，请检查服务配置。"),
                        HomeChatBlock.Text("I've done the check and summarized the result."),
                    ),
                ),
                onContentTap = {},
            )
            Spacer(modifier = Modifier.height(24.dp))
            LiquidChatComposer(
                value = "继续分析",
                onValueChange = {},
                onSendClick = {},
                onStopClick = {},
                isGenerating = false,
                maxLines = 10,
            )
        }
    }
}
