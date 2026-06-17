package com.niki914.nexus.agentic.app.ui.nexus.route

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.niki914.nexus.agentic.app.R
import com.niki914.nexus.agentic.app.conversation.ConversationRepo
import com.niki914.nexus.agentic.app.ui.nexus.PageBackHandler
import com.niki914.nexus.agentic.app.ui.nexus.PageChromeContribution
import com.niki914.nexus.agentic.app.ui.nexus.RegisterPageChrome
import com.niki914.nexus.agentic.app.ui.nexus.content.ConversationHistoryPageContent
import com.niki914.nexus.agentic.app.ui.nexus.content.ConversationHistoryUiState
import com.niki914.nexus.agentic.app.ui.nexus.nav.TopBarActionSpec

@Composable
internal fun ConversationHistoryPageRoute(
    onBack: () -> Unit,
    onConversationSelected: (String) -> Unit,
) {
    var uiState by remember {
        mutableStateOf(ConversationHistoryUiState(isLoading = true))
    }
    val latestOnBack by rememberUpdatedState(onBack)
    val latestOnConversationSelected by rememberUpdatedState(onConversationSelected)
    val backContentDescription = stringResource(
        R.string.ui_conversation_history_back_content_description,
    )

    val pageChromeContribution = remember(backContentDescription) {
        PageChromeContribution(
            rightAction = TopBarActionSpec(
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                onClick = { latestOnBack() },
                contentDescription = backContentDescription,
            ),
            backHandler = PageBackHandler(
                shouldConsumeBack = { true },
                onConsumeBack = { latestOnBack() },
            ),
        )
    }
    RegisterPageChrome(pageChromeContribution)

    LaunchedEffect(Unit) {
        uiState = ConversationHistoryUiState(isLoading = true)
        uiState = runCatching {
            ConversationRepo.listConversations()
        }.fold(
            onSuccess = { conversations ->
                ConversationHistoryUiState(conversations = conversations)
            },
            onFailure = { throwable ->
                ConversationHistoryUiState(
                    errorMessage = throwable.message ?: throwable::class.java.simpleName,
                )
            },
        )
    }

    ConversationHistoryPageContent(
        uiState = uiState,
        onConversationClick = { id ->
            latestOnConversationSelected(id)
        },
    )
}
