package com.niki914.nexus.h.xevent

data class XEventContext(
    val roomId: String? = null,
    val turnId: Long? = null,
    val fields: Map<String, Any?> = emptyMap()
)
