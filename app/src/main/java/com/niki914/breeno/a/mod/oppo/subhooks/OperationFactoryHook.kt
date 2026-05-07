package com.niki914.breeno.a.mod.oppo.subhooks

import com.niki914.breeno.a.mod.BreenoConfigProvider
import com.niki914.breeno.a.mod.ConversationTurnState
import com.niki914.breeno.a.mod.TurnMode
import com.niki914.breeno.h.core.runtime.Hook
import com.niki914.breeno.h.util.call
import com.niki914.breeno.h.util.findClass
import com.niki914.breeno.h.util.hookMethod
import com.niki914.breeno.h.util.resolveParamTypes
import com.niki914.breeno.h.util.xlog
import de.robv.android.xposed.callbacks.XC_LoadPackage

class OperationFactoryHook(
    private val resolveTurnState: (String?) -> ConversationTurnState?
) : Hook {
    override val name: String = "OperationFactoryHook"

    override fun onHook(lpparam: XC_LoadPackage.LoadPackageParam) {
        val factoryClass = BreenoConfigProvider.operationFactoryClass ?: return
        val createMethod = BreenoConfigProvider.operationFactoryCreateMethod ?: return
        val directiveClassName = BreenoConfigProvider.directiveClass ?: return
        val doNothingOperationClass = BreenoConfigProvider.doNothingOperationClass ?: return
        val directiveClass = lpparam.findClass(directiveClassName)
        val getDirectiveRoomIdMethod = BreenoConfigProvider.directiveGetRoomIdMethod ?: return
        val cleanOperationClass = BreenoConfigProvider.cleanOperationClass ?: return

        val createMethodParams = BreenoConfigProvider.operationFactoryCreateMethodParams
        val params = if (createMethodParams != null) {
            resolveParamTypes(createMethodParams, lpparam) ?: return
        } else {
            arrayOf(directiveClass)
        }

        lpparam.hookMethod(
            className = factoryClass,
            methodName = createMethod,
            *params,
            after = after@{ param ->
                val directive = param.args[0]
                val roomId = directive.call<String>(getDirectiveRoomIdMethod)
                if (roomId.isNullOrBlank()) {
                    xlog("[$name] Operation directive 缺失 roomId，保守放行原生 Operation，不回退全局 turnState")
                    return@after
                }

                when (resolveTurnState(roomId)?.mode) {
                    TurnMode.NativeTakeover -> {
                        xlog("[$name] takeover 模式，放行原生 Operation: roomId=$roomId")
                        return@after
                    }

                    TurnMode.InjectedLLM -> Unit
                    null -> {
                        xlog("[$name] 未命中 room 级轮次状态，保守放行原生 Operation: roomId=$roomId")
                        return@after
                    }
                }

                val result = param.result ?: return@after
                val resultClass = result.javaClass
                val isCleanOperation = resultClass.name == cleanOperationClass ||
                    resultClass.simpleName == cleanOperationClass
                if (!isCleanOperation) {
                    return@after
                }

                val classLoader = resultClass.classLoader ?: javaClass.classLoader
                val replacement = Class.forName(doNothingOperationClass, false, classLoader)
                    .getDeclaredConstructor()
                    .newInstance()
                xlog("[$name] 注入模式，CleanOperation -> DoNothingOperation: roomId=$roomId")
                param.result = replacement
            }
        )
    }
}
