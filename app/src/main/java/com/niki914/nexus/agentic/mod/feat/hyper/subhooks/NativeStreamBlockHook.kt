package com.niki914.nexus.agentic.mod.feat.hyper.subhooks

import com.niki914.nexus.agentic.mod.feat.hyper.XiaoaiConfigProvider
import com.niki914.nexus.h.core.runtime.Hook
import com.niki914.nexus.h.util.call
import com.niki914.nexus.h.util.hookMethod
import com.niki914.nexus.h.util.resolveParamTypes
import com.niki914.nexus.h.util.xlog
import de.robv.android.xposed.callbacks.XC_LoadPackage
import org.json.JSONObject

class NativeStreamBlockHook(
    private val shouldBlock: (dialogId: String, payload: JSONObject?) -> Boolean
) : Hook {
    override val name: String = "XiaoaiNativeStreamBlockHook"

    override fun onHook(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (!XiaoaiConfigProvider.blockNativeStreamEnabled) {
            xlog("[$name] action[block_native_stream] 已禁用，跳过安装")
            return
        }

        val ownerClass = XiaoaiConfigProvider.blockNativeStreamOwnerClass ?: return
        val methodName = XiaoaiConfigProvider.blockNativeStreamMethodName ?: return
        val methodParams = XiaoaiConfigProvider.blockNativeStreamMethodParams ?: return
        val dialogIdGetter = XiaoaiConfigProvider.blockNativeStreamDialogIdGetter ?: return
        val hookTiming = XiaoaiConfigProvider.blockNativeStreamHookTiming
        if (hookTiming != null && hookTiming != "before") {
            xlog("[$name] action[block_native_stream] 暂不支持 hook_timing=$hookTiming，跳过安装")
            return
        }

        val paramTypes = resolveParamTypes(methodParams, lpparam) ?: return
        lpparam.hookMethod(
            className = ownerClass,
            methodName = methodName,
            *paramTypes,
            before = before@{ param ->
                val card = param.thisObject ?: return@before
                val dialogId = card.call<String>(dialogIdGetter)
                if (dialogId.isNullOrBlank()) {
                    xlog("[$name] 放行缺失 dialogId 的原生流")
                    return@before
                }

                val payload = param.args.firstOrNull() as? JSONObject
                if (!shouldBlock(dialogId, payload)) {
                    return@before
                }

                xlog("[$name] 拦截原生回答流: dialogId=$dialogId, payload=$payload")
                param.result = null
            }
        )
    }
}
