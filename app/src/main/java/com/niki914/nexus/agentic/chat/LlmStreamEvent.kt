package com.niki914.nexus.agentic.chat

val LlmStreamEvent.isFirst
    get() = this is LlmStreamEvent.RoundStarted
val LlmStreamEvent.isFinal
    get() = this is LlmStreamEvent.Completed
val LlmStreamEvent.chunk
    get() = when (val event = this) {
        is LlmStreamEvent.Error -> "\n\n${event.message}"
        is LlmStreamEvent.TextDelta -> event.delta
        is LlmStreamEvent.ToolFailed -> "\n[tool-failed]"
        is LlmStreamEvent.ToolRunning -> "\n[tool-running]"
        is LlmStreamEvent.ToolSucceeded -> "\n[tool-succeed]"
        else -> ""
    }

sealed interface LlmStreamEvent {
    data object RoundStarted : LlmStreamEvent

    data class TextDelta(
        val delta: String,
        val fullText: String,
        val charsPerSecond: Float? = null,
    ) : LlmStreamEvent

    data class ToolRunning(
        val call: ToolCallStatus,
    ) : LlmStreamEvent

    data class ToolSucceeded(
        val call: ToolCallStatus,
        val outputText: String? = null,
    ) : LlmStreamEvent

    data class ToolFailed(
        val call: ToolCallStatus,
        val message: String,
    ) : LlmStreamEvent

    data class Error(
        val message: String,
        val throwable: Throwable? = null,
    ) : LlmStreamEvent

    data class Completed(
        val fullText: String,
    ) : LlmStreamEvent
}

data class ToolCallStatus(
    val callId: String? = null,
    val name: String,
    val label: String = name,
    val kind: ToolCallKind = ToolCallKind.Unknown,
)

enum class ToolCallKind {
    Local,
    Mcp,
    Unknown,
}
