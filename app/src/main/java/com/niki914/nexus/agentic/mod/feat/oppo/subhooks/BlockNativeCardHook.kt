package com.niki914.nexus.agentic.mod.feat.oppo.subhooks

import com.niki914.nexus.agentic.chat.ConversationTurnState
import com.niki914.nexus.agentic.chat.TurnMode
import com.niki914.nexus.agentic.mod.feat.HookTarget
import com.niki914.nexus.agentic.mod.feat.SubHook
import com.niki914.nexus.agentic.mod.feat.oppo.BreenoConfigProvider
import com.niki914.nexus.h.util.call
import com.niki914.nexus.h.util.xlog
import de.robv.android.xposed.XC_MethodHook

/**
 * 在 InjectedLLM 模式下拦截原生回答卡片，避免 Breeno 侧基于回答卡片的全量刷新注入被原生回答覆盖。
 *
 * 逻辑：
 * - 检查 bean 的 chatType 是否为回答类型（answer），非回答类型直接放行
 * - 检查 bean 是否已包含自注入标记，若已标记则放行（不拦自己注入的卡片）
 * - 通过 roomId 解析当前轮次状态：
 *   - InjectedLLM → 拦截（param.result = null）
 *   - NativeTakeover → 放行
 *   - 无状态 → 保守放行
 */
class BlockNativeCardHook(
    private val resolveTurnState: (String?) -> ConversationTurnState?,
    private val selfInjectedFlagKey: String
) : SubHook() {

    override val hookTarget: HookTarget?
        get() = BreenoConfigProvider.CaptureResponseTarget.hookTarget

    override fun beforeHook(param: XC_MethodHook.MethodHookParam) {
        val bean = param.args[0] ?: return

        val chatType =
            bean.call<Int>(BreenoConfigProvider.CaptureResponseTarget.beanGetChatTypeMethod)
                ?: return
        val typeAnswer = BreenoConfigProvider.CaptureResponseTarget.chatTypeAnswer
        if (chatType != typeAnswer) {
            return
        }

        val isSelfInjected = bean.call<Any>(
            BreenoConfigProvider.CaptureResponseTarget.beanGetClientLocalDataMethod,
            selfInjectedFlagKey
        ) != null
        if (isSelfInjected) {
            xlog("[$name] 放行自定义注入卡片")
            return
        }

        val roomId =
            bean.call<String>(BreenoConfigProvider.CaptureResponseTarget.beanGetRoomIdMethod)
        if (roomId.isNullOrBlank()) {
            xlog("[$name] 回答卡片缺失 roomId，保守放行原生回答卡片")
            return
        }

        when (resolveTurnState(roomId)?.mode) {
            TurnMode.NativeTakeover -> {
                xlog("[$name] takeover 模式，放行原生回答卡片: roomId=$roomId")
            }

            TurnMode.InjectedLLM -> {
                xlog("[$name] 注入模式，拦截原生回答卡片: roomId=$roomId")
                param.result = null
            }

            null -> {
                xlog("[$name] 未命中 room 级轮次状态，保守放行原生回答卡片: roomId=$roomId")
            }
        }
    }
}
