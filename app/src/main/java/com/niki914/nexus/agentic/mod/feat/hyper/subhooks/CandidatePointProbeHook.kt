package com.niki914.nexus.agentic.mod.feat.hyper.subhooks

import android.util.Log
import com.niki914.nexus.h.core.runtime.Hook
import com.niki914.nexus.h.util.call
import com.niki914.nexus.h.util.hookMethod
import com.niki914.nexus.h.util.resolveClass
import com.niki914.nexus.h.util.xTry
import com.niki914.nexus.h.util.xlog
import de.robv.android.xposed.callbacks.XC_LoadPackage

class CandidatePointProbeHook : Hook {
    override val name: String = "XiaoaiCandidatePointProbeHook"

    override fun onHook(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookOperationManagerSpeak(lpparam)
        hookSpeakUrlOperationProcess(lpparam)
        hookSpeakTextOperationCreate(lpparam)
        hookFrontendStreamBridge(lpparam)
    }

    private fun hookOperationManagerSpeak(lpparam: XC_LoadPackage.LoadPackageParam) {
        val instructionClass = resolveClass("com.xiaomi.ai.api.common.Instruction", lpparam) ?: return
        lpparam.hookMethod(
            className = "com.xiaomi.voiceassistant.instruction.base.OperationManager",
            methodName = "onFlowableInstruction",
            instructionClass,
            Boolean::class.javaPrimitiveType ?: return,
            before = before@{ param ->
                val instruction = param.args.firstOrNull() ?: return@before
                val fullName = instruction.call<String>("getFullName").orEmpty()
                if (fullName != "SpeechSynthesizer.Speak") {
                    return@before
                }
                val dialogId = instruction.extractDialogId().orEmpty()
                val payload = instruction.call<Any>("getPayload")
                val text = payload?.call<String>("getText").orEmpty()
                val urlPresent = payload.extractOptionalString("getUrl") != null
                val isOffline = param.args.getOrNull(1) as? Boolean
                CandidatePointProbeLogger.logOnce(
                    point = "OperationManager#onFlowableInstruction",
                    fingerprint = "OperationManager#onFlowableInstruction:Speak:$dialogId",
                    summary = "dialogId=$dialogId, fullName=$fullName, urlPresent=$urlPresent, isOffline=$isOffline, textPreview=${text.preview()}"
                )
            }
        )
    }

    private fun hookSpeakUrlOperationProcess(lpparam: XC_LoadPackage.LoadPackageParam) {
        val procedureClass = resolveClass("g90.l", lpparam) ?: return
        lpparam.hookMethod(
            className = "cb0.n9",
            methodName = "onProcessOp",
            procedureClass,
            before = before@{ param ->
                val operation = param.thisObject ?: return@before
                val instruction = operation.extractInstruction()
                val payload = instruction?.call<Any>("getPayload")
                val dialogId = xTry("probe:getDialogId") { operation.call<String>("getDialogId") }.orEmpty()
                val text = payload?.call<String>("getText").orEmpty()
                val url = payload.extractOptionalString("getUrl")
                CandidatePointProbeLogger.logOnce(
                    point = "cb0.n9#onProcessOp",
                    fingerprint = "cb0.n9#onProcessOp:$dialogId",
                    summary = "dialogId=$dialogId, urlPresent=${url != null}, urlPreview=${url.preview(48)}, textPreview=${text.preview()}"
                )
            }
        )
    }

    private fun hookSpeakTextOperationCreate(lpparam: XC_LoadPackage.LoadPackageParam) {
        val operationContextClass = resolveClass("h90.c", lpparam) ?: return
        lpparam.hookMethod(
            className = "cb0.p9",
            methodName = "onCreateOp",
            operationContextClass,
            before = before@{ param ->
                val operation = param.thisObject ?: return@before
                val instruction = operation.extractInstruction()
                val payload = instruction?.call<Any>("getPayload")
                val dialogId = xTry("probe:getDialogId") { operation.call<String>("getDialogId") }.orEmpty()
                val text = payload?.call<String>("getText").orEmpty()
                val urlPresent = payload.extractOptionalString("getUrl") != null
                CandidatePointProbeLogger.logOnce(
                    point = "cb0.p9#onCreateOp",
                    fingerprint = "cb0.p9#onCreateOp:$dialogId",
                    summary = "dialogId=$dialogId, urlPresent=$urlPresent, textPreview=${text.preview()}"
                )
            }
        )
    }

    private fun hookFrontendStreamBridge(lpparam: XC_LoadPackage.LoadPackageParam) {
        val instructionClass = resolveClass("com.xiaomi.ai.api.common.Instruction", lpparam) ?: return
        lpparam.hookMethod(
            className = "cb0.eb",
            methodName = "A0",
            instructionClass,
            before = before@{ param ->
                val operation = param.thisObject ?: return@before
                val instruction = param.args.firstOrNull() ?: return@before
                val fullName = instruction.call<String>("getFullName").orEmpty()
                val dialogId = xTry("probe:getDialogId") { operation.call<String>("getDialogId") }.orEmpty()
                val payload = instruction.call<Any>("getPayload")
                when (fullName) {
                    "Nlp.UpdateStreamProperties" -> {
                        val simplySpeak = payload?.call<Boolean>("isSimplySpeak")
                        CandidatePointProbeLogger.logOnce(
                            point = "cb0.eb#A0",
                            fingerprint = "cb0.eb#A0:updateProps:$dialogId",
                            summary = "dialogId=$dialogId, fullName=$fullName, simplySpeak=$simplySpeak"
                        )
                    }

                    "Template.ToastStream" -> {
                        val markdown = payload?.call<String>("getMarkdownText").orEmpty()
                        val sampleKind = if (markdown == "<FINAL>") "toast_final" else "toast_first"
                        CandidatePointProbeLogger.logOnce(
                            point = "cb0.eb#A0",
                            fingerprint = "cb0.eb#A0:$sampleKind:$dialogId",
                            summary = "dialogId=$dialogId, fullName=$fullName, markdownLength=${markdown.length}, markdownPreview=${markdown.preview()}"
                        )
                    }

                    "SpeechSynthesizer.SpeakStream" -> {
                        val text = payload?.call<String>("getText").orEmpty()
                        val sampleKind = if (text.isBlank()) "speakstream_empty" else "speakstream_nonempty"
                        CandidatePointProbeLogger.logOnce(
                            point = "cb0.eb#A0",
                            fingerprint = "cb0.eb#A0:$sampleKind:$dialogId",
                            summary = "dialogId=$dialogId, fullName=$fullName, textLength=${text.length}, textPreview=${text.preview()}"
                        )
                    }
                }
            }
        )
    }
}

private object CandidatePointProbeLogger {
    private const val TAG = "nexus-x-log"

    private val lock = Any()
    private val emittedFingerprints = LinkedHashSet<String>()

    fun logOnce(point: String, fingerprint: String, summary: String) {
        val shouldLog = synchronized(lock) {
            emittedFingerprints.add(fingerprint)
        }
        if (!shouldLog) return
        val message = buildString {
            append("[XiaoaiProbe][").append(point).append("] ")
            append(summary)
            append('\n')
            append(buildFilteredStack())
        }
        Log.d(TAG, message)
        xlog(message)
    }

    private fun buildFilteredStack(): String {
        val filtered = Exception("probe").stackTrace
            .filterNot { frame ->
                val className = frame.className
                className.startsWith("android.os.") ||
                    className.startsWith("com.niki914.") ||
                    className.startsWith("java.lang.reflect.") ||
                    className.startsWith("de.robv.android.xposed.") ||
                    className.startsWith("dEgk.") ||
                    className.startsWith("uu0.") ||
                    className.startsWith("ju0.") ||
                    className == "o" ||
                    className == "h" ||
                    className == "android.graphics.DurktPrork"
            }
            .joinToString("\n") { frame ->
                "${frame.className}.${frame.methodName}(${frame.fileName}:${frame.lineNumber})"
            }
        return if (filtered.isBlank()) {
            "<NO_STACK_AFTER_FILTER>"
        } else {
            filtered
        }
    }
}

private fun Any?.extractOptionalString(getterName: String): String? {
    if (this == null) return null
    val direct = xTry("probe:$getterName:direct") { call<String>(getterName) }
    if (!direct.isNullOrBlank()) {
        return direct
    }
    val optional = xTry("probe:$getterName:optional") { call<Any>(getterName) } ?: return null
    val isPresent = xTry("probe:$getterName:isPresent") { optional.call<Boolean>("isPresent") } ?: return null
    if (!isPresent) return null
    return xTry("probe:$getterName:get") { optional.call<String>("get") }
}

private fun Any.extractInstruction(): Any? =
    xTry("probe:getInstruction") { call<Any>("getInstruction") }

private fun Any.extractDialogId(): String? {
    val direct = xTry("probe:getDialogId:direct") { call<String>("getDialogId") }
    if (!direct.isNullOrBlank()) {
        return direct
    }
    val optional = xTry("probe:getDialogId:optional") { call<Any>("getDialogId") } ?: return null
    val isPresent = xTry("probe:getDialogId:isPresent") { optional.call<Boolean>("isPresent") } ?: return null
    if (!isPresent) return null
    return xTry("probe:getDialogId:get") { optional.call<String>("get") }
}

private fun String?.preview(maxLength: Int = 80): String {
    if (this == null) return "null"
    val normalized = replace('\n', ' ')
    return if (normalized.length <= maxLength) {
        normalized
    } else {
        normalized.take(maxLength) + "..."
    }
}
