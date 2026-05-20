package com.niki914.nexus.h.xevent

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class XEventEnvelope(
    val ts: Long,
    val type: XEventType,
    val packageName: String,
    val processName: String,
    val roomId: String?,
    val turnId: Long?,
    val fields: JsonObject
)
