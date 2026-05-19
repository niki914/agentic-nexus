package com.niki914.nexus.agentic.mod.feat.oppo.subhooks

import com.niki914.nexus.agentic.chat.ConversationTurnState
import com.niki914.nexus.agentic.chat.TurnMode
import com.niki914.nexus.agentic.mod.feat.HookTarget
import com.niki914.nexus.agentic.mod.feat.SubHook
import com.niki914.nexus.agentic.mod.feat.oppo.BreenoConfigProvider
import com.niki914.nexus.h.util.xlog
import de.robv.android.xposed.XC_MethodHook

class SuppressCleanupHook(
    private val resolveTurnState: () -> ConversationTurnState?
) : SubHook() {

    override val hookTarget: HookTarget?
        get() = BreenoConfigProvider.SuppressCleanup.hookTarget

    override fun afterHook(param: XC_MethodHook.MethodHookParam) {
        val turnState = resolveTurnState() ?: return
        when (turnState.mode) {
            TurnMode.NativeTakeover -> {
                xlog("[$name] takeover mode, letting native Operation through")
                return
            }
            TurnMode.InjectedLLM -> { /* proceed */ }
        }

        val result = param.result ?: return
        val cleanOperationClass = BreenoConfigProvider.SuppressCleanup.cleanOperationClass
        val doNothingOperationClass = BreenoConfigProvider.SuppressCleanup.doNothingOperationClass

        val resultClass = result.javaClass
        val isCleanOperation = resultClass.name == cleanOperationClass ||
            resultClass.simpleName == cleanOperationClass
        if (!isCleanOperation) {
            return
        }

        val classLoader = resultClass.classLoader ?: javaClass.classLoader
        val replacement = Class.forName(doNothingOperationClass, false, classLoader)
            .getDeclaredConstructor()
            .newInstance()
        xlog("[$name] InjectedLLM mode, CleanOperation -> DoNothingOperation")
        param.result = replacement
    }
}
