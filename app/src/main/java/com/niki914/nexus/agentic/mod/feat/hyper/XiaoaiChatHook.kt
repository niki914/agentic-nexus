package com.niki914.nexus.agentic.mod.feat.hyper

import com.niki914.nexus.agentic.chat.LLMController
import com.niki914.nexus.agentic.chat.TurnMode
import com.niki914.nexus.agentic.mod.feat.AbstractAssistantHook
import com.niki914.nexus.agentic.mod.feat.hyper.subhooks.InputHook
import com.niki914.nexus.agentic.mod.feat.hyper.subhooks.InstructionStreamHook
import com.niki914.nexus.agentic.mod.feat.hyper.subhooks.OperationManagerDialogSummaryHook
import com.niki914.nexus.h.util.call
import com.niki914.nexus.h.util.xTry
import com.niki914.nexus.h.util.xlog
import de.robv.android.xposed.callbacks.XC_LoadPackage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class XiaoaiChatHook(
    scope: CoroutineScope
) : AbstractAssistantHook(scope) {
    override val name: String = "XiaoaiChatHook"

    private val stateLock = Any()
    private var responseTargetOperation: Any? = null
    private var responseTargetDialogId: String? = null
    private var pendingRender: PendingRender? = null
    private var selfInjectedInstruction: Any? = null
    private var lastInjectedContent: String = ""

    private data class PendingRender(
        val turnId: Long,
        val roomId: String,
        val delta: String,
        val isFinal: Boolean
    )

    override fun installSessionHooks(lpparam: XC_LoadPackage.LoadPackageParam) = Unit

    override fun installResponseHooks(lpparam: XC_LoadPackage.LoadPackageParam) {
        OperationManagerDialogSummaryHook().onHook(lpparam)
//        InstructionStreamHook(
//            onOperationObserved = { operation, dialogId, _, _ ->
//                val shouldReplay = synchronized(stateLock) {
//                    responseTargetOperation = operation
//                    responseTargetDialogId = dialogId
//                    pendingRender?.roomId == dialogId
//                }
//
//                if (shouldReplay) {
//                    scope.launch {
//                        replayPendingRenderIfNeeded()
//                    }
//                }
//            },
//            shouldBlock = { dialogId, instruction, instructionFullName ->
//                synchronized(stateLock) {
//                    if (instruction != null && instruction === selfInjectedInstruction) {
//                        return@synchronized false
//                    }
//                    turnState.mode == TurnMode.InjectedLLM &&
//                        dialogId == turnState.roomId &&
//                        instructionFullName == "Template.ToastStream"
//                }
//            }
//        ).onHook(lpparam)
    }

    override fun installInputHooks(
        lpparam: XC_LoadPackage.LoadPackageParam,
        onInput: (roomId: String, query: String) -> Unit
    ) {
        InputHook(onInput = onInput).onHook(lpparam)
    }

    override suspend fun dispatchQueryToLLM(turnId: Long, roomId: String, query: String) {
        synchronized(stateLock) {
            if (responseTargetDialogId != roomId) {
                responseTargetOperation = null
                responseTargetDialogId = null
            }
            pendingRender = null
            lastInjectedContent = ""
        }

        xlog("[$name] 开始分发到 LLM: turnId=$turnId, roomId=$roomId, query=$query")
        LLMController.send(query, scope) { chunk, pos ->
            val isFirst = pos == LLMController.Pos.First
            val isFinal = pos == LLMController.Pos.Final
            renderStreamCard(turnId, roomId, chunk, isFirst, isFinal)
        }
    }

    override suspend fun renderStreamCard(
        turnId: Long,
        roomId: String,
        chunk: String,
        isFirst: Boolean,
        isFinal: Boolean
    ) {
        if (turnState.turnId != turnId || turnState.roomId != roomId || turnState.mode != TurnMode.InjectedLLM) {
            xlog(
                "[$name] 丢弃非当前轮次渲染: roomId=$roomId, turnId=$turnId, activeRoom=${turnState.roomId}, activeTurn=${turnState.turnId}, mode=${turnState.mode}"
            )
            return
        }

        val delta = synchronized(stateLock) {
            val previous = lastInjectedContent
            val next = when {
                chunk == previous -> ""
                chunk.startsWith(previous) -> chunk.substring(previous.length)
                else -> chunk
            }
            lastInjectedContent = chunk
            next
        }

        val target = synchronized(stateLock) {
            val operation = responseTargetOperation
            val dialogId = responseTargetDialogId
            if (dialogId == roomId) {
                pendingRender = null
                operation
            } else {
                val buffered = pendingRender
                    ?.takeIf { it.turnId == turnId && it.roomId == roomId }
                    ?.delta
                    .orEmpty()
                pendingRender = PendingRender(
                    turnId = turnId,
                    roomId = roomId,
                    delta = buffered + delta,
                    isFinal = isFinal
                )
                null
            }
        }

        if (target == null) {
            xlog(
                "[$name] 响应 Operation 尚未就绪，缓存待注入片段: roomId=$roomId, turnId=$turnId, isFirst=$isFirst, isFinal=$isFinal, deltaLength=${delta.length}"
            )
            return
        }

        if (delta.isEmpty() && !isFinal) {
            xlog("[$name] 跳过空增量片段: roomId=$roomId, turnId=$turnId")
            return
        }

        renderToTarget(
            target = target,
            turnId = turnId,
            roomId = roomId,
            delta = delta,
            isFinal = isFinal,
            fromReplay = false
        )
    }

    private suspend fun replayPendingRenderIfNeeded() {
        val snapshot = synchronized(stateLock) {
            val pending = pendingRender
            val target = responseTargetOperation
            val dialogId = responseTargetDialogId
            if (pending == null || target == null || dialogId != pending.roomId) {
                null
            } else {
                pendingRender = null
                Triple(target, dialogId, pending)
            }
        } ?: return

        val (target, _, pending) = snapshot
        if (turnState.turnId != pending.turnId || turnState.roomId != pending.roomId || turnState.mode != TurnMode.InjectedLLM) {
            xlog("[$name] 待渲染片段已过期，放弃补写: roomId=${pending.roomId}, turnId=${pending.turnId}")
            return
        }

        renderToTarget(
            target = target,
            turnId = pending.turnId,
            roomId = pending.roomId,
            delta = pending.delta,
            isFinal = pending.isFinal,
            fromReplay = true
        )
    }

    private fun renderToTarget(
        target: Any,
        turnId: Long,
        roomId: String,
        delta: String,
        isFinal: Boolean,
        fromReplay: Boolean
    ) {
        if (target.javaClass.name != "cb0.eb") {
            xlog("[$name] 响应目标类型不匹配，跳过注入: expected=cb0.eb, actual=${target.javaClass.name}")
            return
        }

        if (delta.isNotEmpty()) {
            val deltaInstruction = buildToastStreamInstruction(target, roomId, delta) ?: return
            invokeStreamInstruction(
                target = target,
                instruction = deltaInstruction,
                roomId = roomId,
                turnId = turnId,
                isFinal = false,
                fromReplay = fromReplay,
                length = delta.length
            )
        }

        if (isFinal) {
            val finalInstruction = buildToastStreamInstruction(target, roomId, "<FINAL>") ?: return
            invokeStreamInstruction(
                target = target,
                instruction = finalInstruction,
                roomId = roomId,
                turnId = turnId,
                isFinal = true,
                fromReplay = fromReplay,
                length = delta.length
            )
        }
    }

    private fun invokeStreamInstruction(
        target: Any,
        instruction: Any,
        roomId: String,
        turnId: Long,
        isFinal: Boolean,
        fromReplay: Boolean,
        length: Int
    ) {
        synchronized(stateLock) {
            selfInjectedInstruction = instruction
        }
        try {
            target.call<Unit>("A0", instruction)
            xlog(
                "[$name] 已注入流指令: roomId=$roomId, turnId=$turnId, isFinal=$isFinal, fromReplay=$fromReplay, length=$length"
            )
        } finally {
            synchronized(stateLock) {
                if (selfInjectedInstruction === instruction) {
                    selfInjectedInstruction = null
                }
            }
        }
    }

    private fun buildToastStreamInstruction(target: Any, dialogId: String, markdownText: String): Any? {
        val classLoader = target.javaClass.classLoader ?: return null
        return xTry("buildToastStreamInstruction") {
            val builderClass = Class.forName("p10.p", false, classLoader)
            val builder = builderClass.getDeclaredConstructor().newInstance()
            builder.call<Any>("generateWithDialogId", dialogId, markdownText, -1)
        }
    }
}
