package com.niki914.nexus.agentic.mod

object ConversationJournal {
    private val lock = Any()
    private val hiddenSummaries = mutableListOf<HiddenTurnSummary>()

    fun appendHiddenSummary(summary: HiddenTurnSummary) {
        synchronized(lock) {
            hiddenSummaries += summary
        }
    }

    fun clearRoom(roomId: String) {
        synchronized(lock) {
            hiddenSummaries.removeAll { it.roomId == roomId }
        }
    }

    fun snapshot(): List<HiddenTurnSummary> = synchronized(lock) {
        hiddenSummaries.toList()
    }
}
