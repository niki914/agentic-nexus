package com.niki914.nexus.agentic.chat

sealed interface TurnMode {
    data object InjectedLLM : TurnMode
    data object NativeTakeover : TurnMode
}
