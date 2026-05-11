package com.niki914.nexus.agentic.mod.oppo.subhooks

import com.niki914.nexus.agentic.mod.BreenoConfigProvider
import com.niki914.nexus.agentic.mod.ConversationTurnState
import com.niki914.nexus.agentic.mod.TurnMode
import com.niki914.nexus.h.core.runtime.Hook
import com.niki914.nexus.h.util.call
import com.niki914.nexus.h.util.hookMethod
import com.niki914.nexus.h.util.resolveParamTypes
import com.niki914.nexus.h.util.xlog
import de.robv.android.xposed.callbacks.XC_LoadPackage

class NativeCardPolicyHook(
    private val viewBeanClassProvider: () -> Class<*>?,
    private val resolveTurnState: (String?) -> ConversationTurnState?
) : Hook {
    override val name: String = "NativeCardPolicyHook"

    override fun onHook(lpparam: XC_LoadPackage.LoadPackageParam) {
        val dataCenterClassName = BreenoConfigProvider.dataCenterClass ?: return
        val dataCenterInsertMessageMethod =
            BreenoConfigProvider.dataCenterInsertMessageMethod ?: return
        val beanClass = viewBeanClassProvider() ?: return

        val typeAnswer = BreenoConfigProvider.typeAnswer ?: return
        val myMockFlagKey = BreenoConfigProvider.selfInjectedMockFlagKey ?: return
        val getChatTypeMethod = BreenoConfigProvider.beanGetChatTypeMethod ?: return
        val getRoomIdMethod = BreenoConfigProvider.beanGetRoomIdMethod ?: return
        val getClientLocalDataMethod = BreenoConfigProvider.beanGetClientLocalDataMethod ?: return

        val insertMessageMethodParams = BreenoConfigProvider.dataCenterInsertMessageMethodParams
        val params = if (insertMessageMethodParams != null) {
            resolveParamTypes(insertMessageMethodParams, lpparam) ?: return
        } else {
            arrayOf(beanClass)
        }

        lpparam.hookMethod(
            className = dataCenterClassName,
            methodName = dataCenterInsertMessageMethod,
            *params,
            before = before@{ param ->
                val bean = param.args[0]
                val chatType = bean.call<Int>(getChatTypeMethod) ?: return@before
                if (chatType != typeAnswer) {
                    return@before
                }

                val isMyMock = bean.call<Any>(getClientLocalDataMethod, myMockFlagKey) != null
                if (isMyMock) {
                    xlog("[$name] 放行自定义注入卡片")
                    return@before
                }

                val roomId = bean.call<String>(getRoomIdMethod)
                if (roomId.isNullOrBlank()) {
                    xlog("[$name] 回答卡片缺失 roomId，保守放行原生回答卡片，不回退全局 turnState")
                    return@before
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
        )
    }
}
