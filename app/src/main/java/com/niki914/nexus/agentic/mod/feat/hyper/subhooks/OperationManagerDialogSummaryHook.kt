package com.niki914.nexus.agentic.mod.feat.hyper.subhooks

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.niki914.nexus.h.core.runtime.Hook
import com.niki914.nexus.h.util.hookMethod
import com.niki914.nexus.h.util.resolveClass
import com.niki914.nexus.h.util.xlog
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.lang.reflect.Method

class OperationManagerDialogSummaryHook : Hook {

    private data class DialogTrace(
        val dialogId: String,
        var offline: Boolean? = null,
        val firstSeenAt: Long = SystemClock.uptimeMillis(),
        var lastSeenAt: Long = firstSeenAt,
        val sequence: MutableList<String> = mutableListOf(),
        val counts: LinkedHashMap<String, Int> = linkedMapOf(),
        val models: LinkedHashMap<String, String> = linkedMapOf(),
        val facts: LinkedHashMap<String, String> = linkedMapOf()
    )

    companion object {
        private const val FLUSH_CHECK_INTERVAL_MS = 800L
        private const val DIALOG_IDLE_FLUSH_MS = 1_500L
        private const val MAX_SEQUENCE_SIZE = 24
        private const val MAX_STRING_VALUE_LENGTH = 64

        private val traceLock = Any()
        private val traces = LinkedHashMap<String, DialogTrace>()
        private val flushHandler by lazy { Handler(Looper.getMainLooper()) }

        @Volatile
        private var printerStarted = false
    }

    override val name: String = "OpMgrDialogSummaryHook"

    override fun onHook(lpparam: XC_LoadPackage.LoadPackageParam) {
        ensurePrinterStarted()
        val instructionClass =
            resolveClass("com.xiaomi.ai.api.common.Instruction", lpparam) ?: return

        lpparam.hookMethod(
            className = "com.xiaomi.voiceassistant.instruction.base.OperationManager",
            methodName = "onFlowableInstruction",
            instructionClass,
            Boolean::class.javaPrimitiveType ?: return,
            before = before@{ param ->
                val instruction = param.args.getOrNull(0) ?: return@before
                val fullName = invokeStringGetter(instruction, "getFullName").orEmpty()
                if (fullName.isBlank()) {
                    return@before
                }

                val dialogId = extractDialogId(instruction)
                if (dialogId.isNullOrBlank()) {
                    return@before
                }

                val payload = invokeGetter(instruction, "getPayload")
                val isOffline = param.args.getOrNull(1) as? Boolean
                recordEvent(
                    dialogId = dialogId,
                    fullName = fullName,
                    payload = payload,
                    isOffline = isOffline
                )
            }
        )
    }

    private fun ensurePrinterStarted() {
        if (printerStarted) return
        synchronized(OperationManagerDialogSummaryHook::class.java) {
            if (printerStarted) return
            flushHandler.postDelayed(object : Runnable {
                override fun run() {
                    flushInactiveDialogs()
                    flushHandler.postDelayed(this, FLUSH_CHECK_INTERVAL_MS)
                }
            }, FLUSH_CHECK_INTERVAL_MS)
            printerStarted = true
        }
    }

    private fun recordEvent(
        dialogId: String,
        fullName: String,
        payload: Any?,
        isOffline: Boolean?
    ) {
        synchronized(traceLock) {
            val trace = traces.getOrPut(dialogId) { DialogTrace(dialogId = dialogId) }
            trace.lastSeenAt = SystemClock.uptimeMillis()
            if (trace.offline == null && isOffline != null) {
                trace.offline = isOffline
            }
            if (trace.sequence.size < MAX_SEQUENCE_SIZE) {
                trace.sequence += fullName
            }
            trace.counts[fullName] = (trace.counts[fullName] ?: 0) + 1
            if (payload != null && !trace.models.containsKey(fullName)) {
                trace.models[fullName] = payload.javaClass.name
            }
            collectFacts(trace, fullName, payload)
        }
    }

    private fun flushInactiveDialogs() {
        val now = SystemClock.uptimeMillis()
        val expired = synchronized(traceLock) {
            val result = traces.values
                .filter { now - it.lastSeenAt >= DIALOG_IDLE_FLUSH_MS }
                .map { it.copyForFlush() }
            result.forEach { traces.remove(it.dialogId) }
            result
        }
        expired.forEach { trace ->
            xlog(buildSummary(trace))
        }
    }

    private fun collectFacts(trace: DialogTrace, fullName: String, payload: Any?) {
        if (payload == null) return
        when (fullName) {
            "Template.Query" -> {
                invokeStringGetter(payload, "getText")
                    ?.let { trace.facts["queryText"] = compactString(it) }
            }

            "Template.ToastStream" -> {
                invokeStringGetter(payload, "getMarkdownText")
                    ?.let { text ->
                        val compact = compactString(text)
                        trace.facts.putIfAbsent("toastFirst", compact)
                        trace.facts["toastLast"] = compact
                    }
            }

            "Nlp.UpdateStreamProperties" -> {
                invokeBooleanGetter(payload, "isSimplySpeak")
                    ?.let { trace.facts["simplySpeak"] = it.toString() }
            }

            "Template.FrontendPage" -> {
                invokeGetter(payload, "getInstructions")
                    ?.javaClass
                    ?.name
                    ?.let { trace.facts["frontendInstructionsType"] = it }
            }
        }
    }

    private fun buildSummary(trace: DialogTrace): String {
        val durationMs = trace.lastSeenAt - trace.firstSeenAt
        val transitions = buildTransitions(trace.sequence)
        return buildString {
            append("dialogSummary:\n")
            append("dialogId=").append(trace.dialogId)
            append(", offline=").append(trace.offline ?: false)
            append(", durationMs=").append(durationMs)
            append('\n')
            append("sequence=").append(formatList(trace.sequence))
            append('\n')
            append("counts=").append(formatMap(trace.counts))
            append('\n')
            append("models=").append(formatMap(trace.models))
            append('\n')
            append("facts=").append(formatMap(trace.facts))
            append('\n')
            append("transitions=").append(formatList(transitions))
        }
    }

    private fun buildTransitions(sequence: List<String>): List<String> {
        if (sequence.size < 2) return emptyList()
        return buildList(sequence.size - 1) {
            for (index in 1 until sequence.size) {
                add("${sequence[index - 1]}->${sequence[index]}")
            }
        }
    }

    private fun formatList(items: List<String>): String =
        items.joinToString(prefix = "[", postfix = "]")

    private fun formatMap(map: Map<String, *>): String =
        map.entries.joinToString(prefix = "{", postfix = "}") { (key, value) ->
            "$key=$value"
        }

    private fun compactString(value: String): String {
        val normalized = value.replace('\n', ' ').trim()
        return if (normalized.length <= MAX_STRING_VALUE_LENGTH) {
            normalized
        } else {
            normalized.take(MAX_STRING_VALUE_LENGTH) + "..."
        }
    }

    private fun extractDialogId(instruction: Any): String? {
        invokeStringGetter(instruction, "getDialogId")
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        val optional = invokeGetter(instruction, "getDialogId") ?: return null
        val isPresent = invokeBooleanGetter(optional, "isPresent") ?: return null
        if (!isPresent) return null
        return invokeStringGetter(optional, "get")
            ?.takeIf { it.isNotBlank() }
    }

    private fun invokeGetter(target: Any, methodName: String): Any? =
        findZeroArgMethod(target.javaClass, methodName)?.let { method ->
            runCatching { method.invoke(target) }.getOrNull()
        }

    private fun invokeStringGetter(target: Any, methodName: String): String? =
        invokeGetter(target, methodName) as? String

    private fun invokeBooleanGetter(target: Any, methodName: String): Boolean? =
        invokeGetter(target, methodName) as? Boolean

    private fun findZeroArgMethod(clazz: Class<*>, methodName: String): Method? =
        clazz.methods.firstOrNull { method ->
            method.name == methodName && method.parameterCount == 0
        }

    private fun DialogTrace.copyForFlush(): DialogTrace =
        DialogTrace(
            dialogId = dialogId,
            offline = offline,
            firstSeenAt = firstSeenAt,
            lastSeenAt = lastSeenAt,
            sequence = sequence.toMutableList(),
            counts = LinkedHashMap(counts),
            models = LinkedHashMap(models),
            facts = LinkedHashMap(facts)
        )
}
