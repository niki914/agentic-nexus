package com.niki914.nexus.agentic.chat

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
    val mode: com.niki914.nexus.agentic.chat.TurnMode = _root_ide_package_.com.niki914.nexus.agentic.chat.TurnMode.InjectedLLM
) {
    fun resetForRoom(roomId: String = "") = ConversationTurnState(roomId = roomId)

    fun nextTurn(roomId: String, query: String, mode: com.niki914.nexus.agentic.chat.TurnMode) = ConversationTurnState(
        roomId = roomId,
        turnId = _root_ide_package_.com.niki914.nexus.agentic.chat.TurnIdGenerator.next(),
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
