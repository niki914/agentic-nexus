package com.niki914.nexus.h.xevent

import android.app.Application
import android.os.Build
import com.niki914.nexus.h.util.ContextProvider
import com.niki914.nexus.h.util.xTry
import com.niki914.nexus.h.util.xtlog
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object XEvent {

    suspend fun inputCaptured(
        roomId: String? = null,
        turnId: Long? = null,
        vararg fields: Pair<String, Any?>
    ) = emit(XEventType.INPUT_CAPTURED, roomId, turnId, mapOf(*fields))

    suspend fun inputCaptured(
        roomId: String? = null,
        turnId: Long? = null,
        fields: Map<String, Any?>
    ) = emit(XEventType.INPUT_CAPTURED, roomId, turnId, fields)

    suspend fun turnDecided(
        roomId: String? = null,
        turnId: Long? = null,
        vararg fields: Pair<String, Any?>
    ) = emit(XEventType.TURN_DECIDED, roomId, turnId, mapOf(*fields))

    suspend fun turnDecided(
        roomId: String? = null,
        turnId: Long? = null,
        fields: Map<String, Any?>
    ) = emit(XEventType.TURN_DECIDED, roomId, turnId, fields)

    suspend fun llmRoundStarted(
        roomId: String? = null,
        turnId: Long? = null,
        vararg fields: Pair<String, Any?>
    ) = emit(XEventType.LLM_ROUND_STARTED, roomId, turnId, mapOf(*fields))

    suspend fun llmRoundStarted(
        roomId: String? = null,
        turnId: Long? = null,
        fields: Map<String, Any?>
    ) = emit(XEventType.LLM_ROUND_STARTED, roomId, turnId, fields)

    suspend fun llmTextDelta(
        roomId: String? = null,
        turnId: Long? = null,
        vararg fields: Pair<String, Any?>
    ) = emit(XEventType.LLM_TEXT_DELTA, roomId, turnId, mapOf(*fields))

    suspend fun llmTextDelta(
        roomId: String? = null,
        turnId: Long? = null,
        fields: Map<String, Any?>
    ) = emit(XEventType.LLM_TEXT_DELTA, roomId, turnId, fields)

    suspend fun llmRoundCompleted(
        roomId: String? = null,
        turnId: Long? = null,
        vararg fields: Pair<String, Any?>
    ) = emit(XEventType.LLM_ROUND_COMPLETED, roomId, turnId, mapOf(*fields))

    suspend fun llmRoundCompleted(
        roomId: String? = null,
        turnId: Long? = null,
        fields: Map<String, Any?>
    ) = emit(XEventType.LLM_ROUND_COMPLETED, roomId, turnId, fields)

    suspend fun llmError(
        roomId: String? = null,
        turnId: Long? = null,
        vararg fields: Pair<String, Any?>
    ) = emit(XEventType.LLM_ERROR, roomId, turnId, mapOf(*fields))

    suspend fun llmError(
        roomId: String? = null,
        turnId: Long? = null,
        fields: Map<String, Any?>
    ) = emit(XEventType.LLM_ERROR, roomId, turnId, fields)

    suspend fun emit(
        type: XEventType,
        roomId: String? = null,
        turnId: Long? = null,
        fields: Map<String, Any?> = emptyMap()
    ) {
        val context = ContextProvider.await()
        val packageName = context.packageName
        val processName = xTry("XEvent#resolveProcessName") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Application.getProcessName()
            } else {
                val activityThreadClass = Class.forName("android.app.ActivityThread")
                val currentProcessNameMethod =
                    activityThreadClass.getDeclaredMethod("currentProcessName")
                currentProcessNameMethod.invoke(null) as String
            }
        } ?: packageName

        val envelope = XEventEnvelope(
            ts = System.currentTimeMillis(),
            type = type,
            packageName = packageName,
            processName = processName,
            roomId = roomId,
            turnId = turnId,
            fields = fields.toJsonObject()
        )

        val jsonString = Json.encodeToString(envelope)

        xtlog("XEvent", jsonString) // TODO P1 py server --> db --> script check
    }
}
