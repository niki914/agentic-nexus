package com.niki914.breeno.a.mod

import com.niki914.breeno.h.core.runtime.Hook
import de.robv.android.xposed.callbacks.XC_LoadPackage
import kotlinx.coroutines.CoroutineScope

/**
 * 抽象语音助手 Hook 基类，规范核心生命周期与功能职责
 *
 * TODO 新对话是否纳入标准
 */
abstract class AbstractAssistantHook(protected val scope: CoroutineScope) : Hook {

    override fun onHook(lpparam: XC_LoadPackage.LoadPackageParam) {
        // 1. 屏蔽官方的原生技能卡片（或放行自定义卡片）
        blockNativeSkill(lpparam)

        // 2. 监听用户输入，并在捕获到输入时进行大模型请求
        interceptInput(lpparam) { query ->
            // 将捕获到的输入交给统一的 LLMController 处理
            dispatchQueryToLLM(query)
        }
    }

    /**
     * 屏蔽原生技能逻辑
     */
    protected abstract fun blockNativeSkill(lpparam: XC_LoadPackage.LoadPackageParam)

    /**
     * 监听输入逻辑，当捕获到用户输入时，调用 [onInput] 回调
     */
    protected abstract fun interceptInput(
        lpparam: XC_LoadPackage.LoadPackageParam,
        onInput: (String) -> Unit
    )

    /**
     * 将查询分发给 LLM SDK
     */
    protected abstract fun dispatchQueryToLLM(query: String)

    /**
     * 渲染流式返回的大模型文本卡片
     * @param chunk 累加后的流式文本块
     * @param isFirst 是否为第一片
     * @param isFinal 是否为最后一片
     */
    protected abstract fun renderStreamCard(chunk: String, isFirst: Boolean, isFinal: Boolean)
}
