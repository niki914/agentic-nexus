package com.niki914.nexus.agentic.mod.feat.hyper.subhooks

import com.niki914.nexus.agentic.mod.feat.hyper.InjectedInstructionRegistry
import com.niki914.nexus.agentic.mod.feat.hyper.ResponseTargetStore
import com.niki914.nexus.agentic.mod.feat.hyper.XiaoaiConfigProvider
import com.niki914.nexus.agentic.mod.feat.hyper.XiaoaiRenderSession
import com.niki914.nexus.h.core.runtime.Hook
import com.niki914.nexus.h.util.call
import com.niki914.nexus.h.util.xlog
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.util.concurrent.atomic.AtomicLong

class RenderTextStreamCardHook(
    private val responseTargetStore: ResponseTargetStore,
    private val injectedInstructionRegistry: InjectedInstructionRegistry
) : Hook {
    override val name: String = "XiaoaiRenderTextStreamCardHook"

    private val instructionCounter = AtomicLong()
    private val sessionLock = Any()
    private var currentSession: XiaoaiRenderSession? = null
    private var renderMethodName: String? = null

    override fun onHook(lpparam: XC_LoadPackage.LoadPackageParam) {
        val ownerClass = XiaoaiConfigProvider.renderTextStreamCardOwnerClass ?: return
        val methodName = XiaoaiConfigProvider.renderTextStreamCardMethodName ?: return
        val methodParams = XiaoaiConfigProvider.renderTextStreamCardMethodParams ?: return
        renderMethodName = methodName
        xlog("[$name] 已就绪: target=$ownerClass#$methodName(${methodParams.joinToString()})")
    }

    fun render(
        turnId: Long,
        dialogId: String,
        chunk: String,
        isFirst: Boolean,
        isFinal: Boolean
    ) {
        val methodName = renderMethodName ?: return
        val session = obtainSession(turnId, dialogId)
        val delta = synchronized(sessionLock) {
            val previous = session.renderedText
            val nextDelta = when {
                chunk.isEmpty() -> ""
                chunk.startsWith(previous) -> chunk.removePrefix(previous)
                else -> chunk
            }
            session.renderedText = chunk
            nextDelta
        }

        val target = responseTargetStore.get(dialogId)
        if (target == null) {
            xlog("[$name] 未捕获到可用响应目标，暂不注入: dialogId=$dialogId")
            if (isFinal) {
                clearSession(turnId)
            }
            return
        }

        if (delta.isNotEmpty()) {
            injectChunk(target, methodName, dialogId, delta)
        } else if (isFirst && !isFinal) {
            xlog("[$name] 首帧为空，等待后续增量: dialogId=$dialogId")
        }

        if (isFinal) {
            injectChunk(
                target = target,
                methodName = methodName,
                dialogId = dialogId,
                text = XiaoaiConfigProvider.renderTextStreamCardFinalChunkText
            )
            clearSession(turnId)
        }
    }

    private fun injectChunk(
        target: Any,
        methodName: String,
        dialogId: String,
        text: String
    ) {
        val classLoader = target.javaClass.classLoader ?: javaClass.classLoader
        val instructionClass = Class.forName("com.xiaomi.ai.api.common.Instruction", false, classLoader)
        val headerClass = Class.forName("com.xiaomi.ai.api.common.InstructionHeader", false, classLoader)
        val payloadClass = Class.forName("com.xiaomi.ai.api.Template\$ToastStream", false, classLoader)

        val instruction = instructionClass.getDeclaredConstructor().newInstance()
        val header = headerClass
            .getDeclaredConstructor(String::class.java, String::class.java)
            .newInstance(
                XiaoaiConfigProvider.renderTextStreamCardInstructionNamespace,
                XiaoaiConfigProvider.renderTextStreamCardInstructionName
            )
        val payload = payloadClass.getDeclaredConstructor(String::class.java).newInstance(text)
        val instructionId = buildInstructionId()

        header.call<Any>("setId", instructionId)
        header.call<Any>("setDialogId", dialogId)
        instruction.call<Unit>("setHeader", header)
        instruction.call<Unit>("setPayload", payload)
        injectedInstructionRegistry.markInjected(instruction)
        target.call<Unit>(methodName, instruction)
        xlog("[$name] 已注入文字流分片: dialogId=$dialogId, text=$text")
    }

    private fun buildInstructionId(): String {
        val next = instructionCounter.incrementAndGet()
        return "${XiaoaiConfigProvider.renderTextStreamCardInstructionIdPrefix}_$next"
    }

    private fun obtainSession(turnId: Long, dialogId: String): XiaoaiRenderSession = synchronized(sessionLock) {
        currentSession?.takeIf { it.turnId == turnId && it.dialogId == dialogId }
            ?: XiaoaiRenderSession(turnId = turnId, dialogId = dialogId).also { currentSession = it }
    }

    private fun clearSession(turnId: Long) = synchronized(sessionLock) {
        if (currentSession?.turnId == turnId) {
            currentSession = null
        }
    }
}
