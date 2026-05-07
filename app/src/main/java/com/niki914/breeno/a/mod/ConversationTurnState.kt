package com.niki914.breeno.a.mod

import java.util.concurrent.atomic.AtomicLong

enum class TurnMode {
    InjectedLLM,
    NativeTakeover
}

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
    fun resetForRoom(roomId: String = "") = ConversationTurnState(roomId = roomId)

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
