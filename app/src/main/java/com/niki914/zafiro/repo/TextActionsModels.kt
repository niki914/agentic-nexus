package com.niki914.zafiro.repo

/**
 * 一条文本处理动作：从系统文本选择菜单触发，用 [promptTemplate] 包装选中文本后发起新对话。
 *
 * 模型只存用户可见的字段；「后台静默」「直接发送」是全局设置（app.state），
 * 不随单条动作走。
 */
data class TextAction(
    val id: String,
    val name: String,
    val promptTemplate: String,
    val enabled: Boolean = true,
)

/** 6 条内置动作；名称与模板均为英文硬编码，用户可编辑。 */
internal val defaultTextActions: List<TextAction> = listOf(
    TextAction(
        id = "explain",
        name = "Explain",
        promptTemplate = "Explain the following text in simple terms.",
    ),
    TextAction(
        id = "translate",
        name = "Translate",
        promptTemplate = "Translate the following text into English.",
    ),
    TextAction(
        id = "research",
        name = "Research",
        promptTemplate = "Research the topic of the following text and give me a brief summary.",
    ),
    TextAction(
        id = "summarize",
        name = "Summarize",
        promptTemplate = "Summarize the following text in a few bullet points.",
    ),
    TextAction(
        id = "rewrite",
        name = "Rewrite",
        promptTemplate = "Rewrite the following text to improve clarity and flow.",
    ),
    TextAction(
        id = "reply",
        name = "Reply",
        promptTemplate = "Draft a short reply to the following message.",
    ),
)
