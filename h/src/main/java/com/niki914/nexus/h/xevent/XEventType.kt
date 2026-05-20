package com.niki914.nexus.h.xevent

import kotlinx.serialization.Serializable

@Serializable
enum class XEventType {
    INPUT_CAPTURED,
    TURN_DECIDED,
    LLM_ROUND_STARTED,
    LLM_TEXT_DELTA,
    LLM_ROUND_COMPLETED,
    LLM_ERROR
}
