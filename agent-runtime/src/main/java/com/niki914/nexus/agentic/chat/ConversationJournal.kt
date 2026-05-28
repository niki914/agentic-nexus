package com.niki914.nexus.agentic.chat

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object ConversationJournal {
    private val mutex = Mutex()
    private val hiddenSummaries = mutableListOf<HiddenTurnSummary>()

    suspend fun appendHiddenSummary(summary: HiddenTurnSummary) {
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
