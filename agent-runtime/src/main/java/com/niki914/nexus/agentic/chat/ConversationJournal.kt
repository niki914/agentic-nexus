package com.niki914.nexus.agentic.chat

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object ConversationJournal {
    private val mutex = Mutex()
    private val hiddenSummaries = mutableListOf<com.niki914.nexus.agentic.chat.HiddenTurnSummary>()

    suspend fun appendHiddenSummary(summary: com.niki914.nexus.agentic.chat.HiddenTurnSummary) {
        mutex.withLock {
            hiddenSummaries += summary
        }
    }

    suspend fun clearRoom(roomId: String) {
        mutex.withLock {
            hiddenSummaries.removeAll { it.roomId == roomId }
        }
    }
}
