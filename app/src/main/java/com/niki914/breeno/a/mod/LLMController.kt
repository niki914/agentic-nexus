package com.niki914.breeno.a.mod

import com.niki914.s3ss10n.ChatSession
import com.niki914.s3ss10n.chat.AIContent
import com.niki914.s3ss10n.chat.protocol.ToolCall
import com.niki914.s3ss10n.chat.protocol.beans.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 统一的大模型 SDK 调度中心
 */
object LLMController {
    /**
     * 请求流式返回
     * @param query 用户的输入
     * @param scope 协程作用域
     * @param onChunk 收到数据块的回调，参数分别为: chunkText, isFirst, isFinal
     */
    fun requestStream(
        query: String,
        scope: CoroutineScope,
        onChunk: (String, Boolean, Boolean) -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            val session = ChatSession().apply {
                callback = object : ChatSession.Callback {
                    private val accumulator = java.lang.StringBuilder()
                    private var isFirstChunk = true

                    override fun onConfigInvalid() {
                    }

                    override fun onStarted() { //  TODO 调整 SDK 语义：是请求开始开始首字？
                    }

                    override fun onUpdated() {
                    }

                    override fun onContent(aiContent: AIContent) {
                        when (aiContent) {
                            is AIContent.Else -> {}
                            is AIContent.Text -> {
                                val delta = aiContent.content
                                accumulator.append(delta)
                                if (isFirstChunk && delta.isEmpty()) return
                                onChunk(accumulator.toString(), isFirstChunk, false)
                                isFirstChunk = false
                            }
                        }
                    }

                    override fun onError(message: String, cause: Throwable?) {
                        accumulator.append("\n[Error: $message]")
                        onChunk(accumulator.toString(), isFirstChunk, true)
                    }

                    override suspend fun onToolCall(toolCall: ToolCall): Message.Tool {
                        return Message.Tool(
                            toolCallId = toolCall.id ?: "tool_call_id",
                            name = toolCall.function?.name ?: "tool_name",
                            content = "{}"
                        )
                    }

                    override fun onCompleted(isSuccess: Boolean, cause: Throwable?) {
                        onChunk(accumulator.toString(), isFirstChunk, true)
                    }
                }
            }

            session.updateConfig {
                // 使用 Entrance 中配置的 URL 等参数，此处按要求写死或引用 Entrance 常量
                baseUrl = Entrance.LLM_BASE_URL
                apiKey = Entrance.LLM_API_KEY
                modelName = Entrance.LLM_MODEL_NAME
                prompt = "You are a helpful assistant."
            }

//            session.preConnect()
            session.sendMessage(query)
        }
    }
}
