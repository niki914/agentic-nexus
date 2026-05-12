package com.niki914.nexus.agentic.mod.feat.oppo.subhooks

import com.niki914.nexus.agentic.mod.feat.oppo.BreenoConfigProvider
import com.niki914.nexus.agentic.chat.ConversationTurnState
import com.niki914.nexus.agentic.chat.TurnMode
import com.niki914.nexus.h.core.runtime.Hook
import com.niki914.nexus.h.util.findClass
import com.niki914.nexus.h.util.hookMethod
import com.niki914.nexus.h.util.resolveParamTypes
import com.niki914.nexus.h.util.xlog
import de.robv.android.xposed.callbacks.XC_LoadPackage

class OperationFactoryHook(
    private val resolveTurnState: () -> ConversationTurnState
) : Hook {
    override val name: String = "OperationFactoryHook"

    override fun onHook(lpparam: XC_LoadPackage.LoadPackageParam) {
        val factoryClass = BreenoConfigProvider.operationFactoryClass ?: return
        val createMethod = BreenoConfigProvider.operationFactoryCreateMethod ?: return
        val directiveClassName = BreenoConfigProvider.directiveClass ?: return
        val doNothingOperationClass = BreenoConfigProvider.doNothingOperationClass ?: return
        val directiveClass = lpparam.findClass(directiveClassName)
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
                when (resolveTurnState().mode) {
                    TurnMode.NativeTakeover -> {
                        xlog("[$name] takeover 模式，放行原生 Operation")
                        return@after
                    }

                    TurnMode.InjectedLLM -> Unit
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
                xlog("[$name] 注入模式，CleanOperation -> DoNothingOperation")
                param.result = replacement
            }
        )
    }
}
