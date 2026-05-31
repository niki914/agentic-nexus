package com.niki914.nexus.agentic.chat

import java.util.concurrent.atomic.AtomicReference

object ActiveTurnStore {
    private val current = AtomicReference<ConversationTurnState?>(null)

    fun getCurrent(): ConversationTurnState? = current.get()

    fun setCurrent(state: ConversationTurnState) {
        current.set(state)
    }

    fun clear(roomId: String = "") {
        current.set(null)
    }

    fun isCurrentInjected(): Boolean = getCurrent()?.mode == TurnMode.InjectedLLM

    fun hasActiveTurn(): Boolean = getCurrent() != null
}
