package com.niki914.breeno.a.mod.oppo.subhooks

import com.niki914.breeno.a.mod.BreenoConfigProvider
import com.niki914.breeno.a.mod.ConversationTurnState
import com.niki914.breeno.a.mod.TurnMode
import com.niki914.breeno.h.core.runtime.Hook
import com.niki914.breeno.h.util.call
import com.niki914.breeno.h.util.hookMethod
import com.niki914.breeno.h.util.resolveParamTypes
import com.niki914.breeno.h.util.xlog
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
        val myMockFlagKey = BreenoConfigProvider.selfInjectedMockFlagKey
        val getChatTypeMethod = BreenoConfigProvider.beanGetChatTypeMethod
        val getRoomIdMethod = BreenoConfigProvider.beanGetRoomIdMethod
        val getClientLocalDataMethod = BreenoConfigProvider.beanGetClientLocalDataMethod

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
