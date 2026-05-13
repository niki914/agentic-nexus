package com.niki914.nexus.agentic.mod.feat.hyper.subhooks
import android.os.Handler
import android.os.Looper

import com.niki914.nexus.h.core.runtime.Hook
import com.niki914.nexus.h.util.call
import com.niki914.nexus.h.util.hookMethod
import com.niki914.nexus.h.util.xTry
import com.niki914.nexus.h.util.resolveClass
import com.niki914.nexus.h.util.xlog
import java.lang.reflect.Modifier
import de.robv.android.xposed.callbacks.XC_LoadPackage

class FlowableInstructionProbeHook : Hook {

    private data class PayloadSnapshot(
        val fullName: String,
        var count: Int = 0,
        var dialogId: String? = null,
        var payloadClassName: String? = null,
        var payloadSummary: String? = null,
        var payloadStructure: String? = null,
        var isOffline: Boolean? = null
    )

    companion object {
        private const val FLUSH_INTERVAL_MS = 1_000L
        private const val MAX_METHOD_COUNT = 24
        private const val MAX_VALUE_LENGTH = 160

        private val aggregateLock = Any()
        private val snapshots = LinkedHashMap<String, PayloadSnapshot>()
        private val flushHandler by lazy { Handler(Looper.getMainLooper()) }

        @Volatile
        private var printerStarted = false
    }
    override val name: String = "XiaoaiFlowableInstructionProbeHook"

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
                val fullName = instruction.call<String>("getFullName").orEmpty()
                val dialogId = extractDialogId(instruction)
                val payload = instruction.call<Any>("getPayload")
                val isOffline = param.args.getOrNull(1) as? Boolean

                recordSnapshot(
                    fullName = fullName.ifBlank { "<EMPTY_FULL_NAME>" },
                    dialogId = dialogId,
                    payload = payload,
                    isOffline = isOffline
                )
            }
        )
    }
    private fun ensurePrinterStarted() {
        if (printerStarted) return
        synchronized(FlowableInstructionProbeHook::class.java) {
            if (printerStarted) return
            flushHandler.postDelayed(object : Runnable {
                override fun run() {
                    flushSnapshots()
                    flushHandler.postDelayed(this, FLUSH_INTERVAL_MS)
                }
            }, FLUSH_INTERVAL_MS)
            printerStarted = true
        }
    }

    private fun recordSnapshot(
        fullName: String,
        dialogId: String?,
        payload: Any?,
        isOffline: Boolean?
    ) {
        synchronized(aggregateLock) {
            val snapshot = snapshots.getOrPut(fullName) {
                PayloadSnapshot(fullName = fullName)
            }
            snapshot.count += 1
            if (!dialogId.isNullOrBlank()) {
                snapshot.dialogId = dialogId
            }
            if (payload != null) {
                snapshot.payloadClassName = payload.javaClass.name
                if (snapshot.payloadSummary == null) {
                    snapshot.payloadSummary = describePayloadSummary(payload)
                }
                if (snapshot.payloadStructure == null) {
                    snapshot.payloadStructure = describePayloadStructure(payload)
                }
            }
            if (isOffline != null) {
                snapshot.isOffline = isOffline
            }
        }
    }

    private fun flushSnapshots() {
        val pending = synchronized(aggregateLock) {
            if (snapshots.isEmpty()) {
                emptyList()
            } else {
                val copied = snapshots.values.map { it.copy() }
                snapshots.clear()
                copied
            }
        }
        if (pending.isEmpty()) return

        pending.forEach { snapshot ->
            xlog(
                buildString {
                    append("[").append(name).append("] onFlowableInstruction aggregate")
                    append(": fullName=").append(snapshot.fullName)
                    append(", count=").append(snapshot.count)
                    append(", dialogId=").append(snapshot.dialogId)
                    append(", payload=").append(snapshot.payloadClassName)
                    append(", isOffline=").append(snapshot.isOffline)
                    append('\n')
                    append("payloadSummary=").append(snapshot.payloadSummary)
                    append('\n')
                    append("payloadStructure=").append(snapshot.payloadStructure)
                }
            )
        }
    }

    private fun extractDialogId(instruction: Any): String? {
        instruction.call<String>("getDialogId")
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        val dialogIdOptional = instruction.call<Any>("getDialogId") ?: return null
        val isPresent = dialogIdOptional.call<Boolean>("isPresent") ?: return null
        if (!isPresent) return null
        return dialogIdOptional.call<String>("get")?.takeIf { it.isNotBlank() }
    }

    private fun describePayloadSummary(payload: Any): String {
        val getters = payload.javaClass.methods
            .asSequence()
            .filter { Modifier.isPublic(it.modifiers) }
            .filter { it.parameterCount == 0 }
            .filter { it.name != "getClass" }
            .filter { it.name.startsWith("get") || it.name.startsWith("is") }
            .sortedBy { it.name }
            .take(MAX_METHOD_COUNT)
            .toList()

        if (getters.isEmpty()) {
            return "<NO_PUBLIC_GETTERS>"
        }

        return getters.joinToString(", ") { method ->
            val value = xTry("payloadSummary:${payload.javaClass.name}#${method.name}") {
                method.invoke(payload)
            }
            "${method.name}=${formatValue(value)}"
        }
    }

    private fun describePayloadStructure(payload: Any): String {
        val getters = payload.javaClass.methods
            .asSequence()
            .filter { Modifier.isPublic(it.modifiers) }
            .filter { it.parameterCount == 0 }
            .filter { it.name != "getClass" }
            .filter { it.name.startsWith("get") || it.name.startsWith("is") }
            .sortedBy { it.name }
            .take(MAX_METHOD_COUNT)
            .toList()

        if (getters.isEmpty()) {
            return "<NO_PUBLIC_GETTERS>"
        }

        return getters.joinToString(", ") { method ->
            "${method.name}:${method.returnType.name}"
        }
    }

    private fun formatValue(value: Any?): String = when (value) {
        null -> "null"
        is String -> value.replace('\n', ' ').take(MAX_VALUE_LENGTH)
        is Number, is Boolean -> value.toString()
        is Enum<*> -> value.name
        else -> value.javaClass.name
    } }
