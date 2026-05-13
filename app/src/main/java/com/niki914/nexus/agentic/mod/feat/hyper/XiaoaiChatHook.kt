package com.niki914.nexus.agentic.mod.feat.hyper

import com.niki914.nexus.agentic.mod.feat.AbstractAssistantHook
import com.niki914.nexus.agentic.mod.feat.hyper.subhooks.InputHook
import com.niki914.nexus.h.util.xlog
import de.robv.android.xposed.callbacks.XC_LoadPackage
import kotlinx.coroutines.CoroutineScope

class XiaoaiChatHook(
    scope: CoroutineScope
) : AbstractAssistantHook(scope) {
    override val name: String = "XiaoaiChatHook"

    override fun installSessionHooks(lpparam: XC_LoadPackage.LoadPackageParam) = Unit

    override fun installResponseHooks(lpparam: XC_LoadPackage.LoadPackageParam) = Unit

    override fun installInputHooks(
        lpparam: XC_LoadPackage.LoadPackageParam,
        onInput: (roomId: String, query: String) -> Unit
    ) {
        InputHook(onInput = onInput).onHook(lpparam)
    }

    override suspend fun dispatchQueryToLLM(turnId: Long, roomId: String, query: String) {
        xlog("[$name] 暂不分发到 LLM: turnId=$turnId, roomId=$roomId, query=$query")
    }

    override suspend fun renderStreamCard(
        turnId: Long,
        roomId: String,
        chunk: String,
        isFirst: Boolean,
        isFinal: Boolean
    ) {
        Unit
    }
}
