package com.niki914.breeno.a.mod

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 模拟统一的大模型 SDK 调度中心
 */
object LLMController {
    /**
     * 模拟请求流式返回
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
            delay(500) // 模拟网络延迟

            val mockData = listOf(
                "你好！", "我是由", " OpenHook ", "强力驱动的", "自定义", "大模型助手！\n\n",
                "我拦截了官方的回复，", "并且使用了", "最优雅的", "领域模型注入方案。", "你说了: ", query
            )

            val accumulator = StringBuilder()

            mockData.forEachIndexed { index, segment ->
                accumulator.append(segment)
                val isFirst = (index == 0)
                val isFinal = (index == mockData.size - 1)

                onChunk(accumulator.toString(), isFirst, isFinal)
                delay(150)
            }
        }
    }
}
