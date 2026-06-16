package com.niki914.nexus.agentic.chat.agentic.buildin.impl

enum class TerminalAction {
    OPEN,
    OPEN_AND_EXEC,
    EXEC,
    READ_ASYNC_RESULT,
    CLOSE,
    ;

    companion object {
        fun from(raw: String?): TerminalAction {
            return when (raw?.trim()?.lowercase()) {
                "open" -> OPEN
                "open_and_exec" -> OPEN_AND_EXEC
                "exec" -> EXEC
                "read_async_result" -> READ_ASYNC_RESULT
                "close" -> CLOSE
                else -> throw IllegalArgumentException(
                    "Field 'action' must be one of open, open_and_exec, exec, read_async_result, close."
                )
            }
        }
    }
}
