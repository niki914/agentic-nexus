package com.niki914.breeno.a.mod.oppo.subhooks

import com.niki914.breeno.a.mod.BreenoConfigProvider
import com.niki914.breeno.h.core.runtime.Hook
import com.niki914.breeno.h.util.call
import com.niki914.breeno.h.util.hookMethod
import com.niki914.breeno.h.util.resolveParamTypes
import com.niki914.breeno.h.util.xlog
import de.robv.android.xposed.callbacks.XC_LoadPackage

class InputHook(
    private val viewBeanClassProvider: () -> Class<*>?,
    private val onDataCenterInstanceResolved: (Any) -> Unit,
    private val onInput: (roomId: String, query: String) -> Unit
) : Hook {
    override val name: String = "InputHook"

    override fun onHook(lpparam: XC_LoadPackage.LoadPackageParam) {
        val dataCenterClassName = BreenoConfigProvider.dataCenterClass ?: return
        val dataCenterInsertMessageMethod =
            BreenoConfigProvider.dataCenterInsertMessageMethod ?: return
        val beanClass = viewBeanClassProvider() ?: return

        val typeQuery = BreenoConfigProvider.typeQuery ?: return
        val getChatTypeMethod = BreenoConfigProvider.beanGetChatTypeMethod
        val getRoomIdMethod = BreenoConfigProvider.beanGetRoomIdMethod
        val getContentMethod = BreenoConfigProvider.beanGetContentMethod

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
                onDataCenterInstanceResolved(param.thisObject)

                val chatType = bean.call<Int>(getChatTypeMethod) ?: return@before
                val roomId = bean.call<String>(getRoomIdMethod) ?: return@before

                if (chatType == typeQuery) {
                    val query = bean.call<String>(getContentMethod)
                    xlog("[$name] 捕获用户输入: $query (roomId=$roomId)")

                    if (!query.isNullOrBlank()) {
                        onInput(roomId, query)
                    }
                }
            }
        )
    }
}
