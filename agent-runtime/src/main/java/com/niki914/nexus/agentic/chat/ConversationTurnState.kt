package com.niki914.nexus.agentic.chat

import java.util.concurrent.atomic.AtomicLong

data class HiddenTurnSummary(
    val turnId: Long,
    val roomId: String,
    val query: String,
    val summary: String
)

data class ConversationTurnState(
    val roomId: String = "",
    val turnId: Long = 0L,
    val lastQuery: String = "",
    val mode: TurnMode = TurnMode.InjectedLLM
) {
    fun nextTurn(roomId: String, query: String, mode: TurnMode) = ConversationTurnState(
        roomId = roomId,
        turnId = TurnIdGenerator.next(),
        lastQuery = query,
        mode = mode
    )
}

private object TurnIdGenerator {
    private val nextId = AtomicLong(System.currentTimeMillis())

    fun next(): Long = nextId.updateAndGet { previous ->
        maxOf(previous + 1L, System.currentTimeMillis())
    }
}
