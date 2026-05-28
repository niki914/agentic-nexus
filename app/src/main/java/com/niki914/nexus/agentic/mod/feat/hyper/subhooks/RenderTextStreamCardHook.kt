package com.niki914.nexus.agentic.mod.feat.hyper.subhooks

import com.niki914.nexus.agentic.mod.feat.SubHook
import com.niki914.nexus.agentic.mod.feat.hyper.XiaoaiConfigProvider
import com.niki914.nexus.agentic.mod.feat.hyper.XiaoaiRenderSession
import com.niki914.nexus.h.util.call
import com.niki914.nexus.h.util.setTag
import com.niki914.nexus.h.util.xlog
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong

/** 将 LLM 的累计文本切成增量块，构造为宿主 Instruction 并注入响应目标，管理渲染会话生命周期与终帧补片。 */
class RenderTextStreamCardHook : SubHook() {

    private val instructionCounter = AtomicLong()
    private val sessionLock = Mutex()
    private var currentSession: XiaoaiRenderSession? = null
    private val renderMethodName: String? by lazy {
        XiaoaiConfigProvider.RenderTextStreamCard.hookTarget?.methodName
    }

    suspend fun render(
        turnId: Long,
        dialogId: String,
        target: Any?,
        chunk: String,
        isFirst: Boolean,
        isFinal: Boolean
    ) {
        val methodName = renderMethodName ?: return
        val delta = sessionLock.withLock {
            val session = obtainSessionLocked(turnId, dialogId)
            val previous = session.renderedText
            val nextDelta = when {
                chunk.isEmpty() -> ""
                chunk.startsWith(previous) -> chunk.removePrefix(previous)
                else -> chunk
            }
            session.renderedText = chunk
            nextDelta
        }

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
                text = XiaoaiConfigProvider.RenderTextStreamCard.finalChunkText
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
        val instruction = newInstance(
            className = XiaoaiConfigProvider.RenderTextStreamCard.instructionClass,
            constructorParamTypes = XiaoaiConfigProvider.RenderTextStreamCard.instructionConstructorParamTypes,
            classLoader = classLoader
        )
        val header = newInstance(
            className = XiaoaiConfigProvider.RenderTextStreamCard.instructionHeaderClass,
            constructorParamTypes = XiaoaiConfigProvider.RenderTextStreamCard.instructionHeaderConstructorParamTypes,
            classLoader = classLoader,
            XiaoaiConfigProvider.RenderTextStreamCard.instructionNamespace,
            XiaoaiConfigProvider.RenderTextStreamCard.instructionName
        )
        val payload = newInstance(
            className = XiaoaiConfigProvider.RenderTextStreamCard.textStreamPayloadClass,
            constructorParamTypes = XiaoaiConfigProvider.RenderTextStreamCard.textStreamPayloadConstructorParamTypes,
            classLoader = classLoader,
            text
        )
        val instructionId = buildInstructionId()

        header.call<Any>(
            XiaoaiConfigProvider.RenderTextStreamCard.instructionHeaderIdSetter,
            instructionId
        )
        header.call<Any>(
            XiaoaiConfigProvider.RenderTextStreamCard.instructionHeaderDialogIdSetter,
            dialogId
        )
        instruction.call<Unit>(
            XiaoaiConfigProvider.RenderTextStreamCard.instructionHeaderSetter,
            header
        )
        instruction.call<Unit>(
            XiaoaiConfigProvider.RenderTextStreamCard.instructionPayloadSetter,
            payload
        )
        instruction.setTag(INJECTED_FLAG, true)
        target.call<Unit>(methodName, instruction)
        xlog("[$name] 已注入文字流分片: dialogId=$dialogId, text=$text")
    }

    private fun newInstance(
        className: String,
        constructorParamTypes: List<String>,
        classLoader: ClassLoader?,
        vararg args: Any?
    ): Any {
        val clazz = resolveRuntimeClass(className, classLoader)
        val paramTypes =
            constructorParamTypes.map { resolveRuntimeClass(it, classLoader) }.toTypedArray()
        return clazz.getDeclaredConstructor(*paramTypes).newInstance(*args)
    }

    private fun resolveRuntimeClass(typeName: String, classLoader: ClassLoader?): Class<*> {
        return runtimePrimitiveTypes[typeName] ?: Class.forName(typeName, false, classLoader)
    }

    private fun buildInstructionId(): String {
        val next = instructionCounter.incrementAndGet()
        return "${XiaoaiConfigProvider.RenderTextStreamCard.instructionIdPrefix}_$next"
    }

    private fun obtainSessionLocked(turnId: Long, dialogId: String): XiaoaiRenderSession =
        currentSession?.takeIf { it.turnId == turnId && it.dialogId == dialogId }
            ?: XiaoaiRenderSession(turnId = turnId, dialogId = dialogId).also {
                currentSession = it
            }

    suspend fun reset() {
        sessionLock.withLock {
            currentSession = null
        }
    }

    private suspend fun clearSession(turnId: Long) {
        sessionLock.withLock {
            if (currentSession?.turnId == turnId) {
                currentSession = null
            }
        }
    }

    companion object {
        private val runtimePrimitiveTypes = mapOf(
            "boolean" to Boolean::class.javaPrimitiveType!!,
            "byte" to Byte::class.javaPrimitiveType!!,
            "char" to Char::class.javaPrimitiveType!!,
            "short" to Short::class.javaPrimitiveType!!,
            "int" to Int::class.javaPrimitiveType!!,
            "long" to Long::class.javaPrimitiveType!!,
            "float" to Float::class.javaPrimitiveType!!,
            "double" to Double::class.javaPrimitiveType!!,
            "void" to Void.TYPE
        )
    }
}
