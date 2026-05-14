package com.niki914.nexus.agentic.mod.feat.hyper

data class XiaoaiRenderSession(
    val turnId: Long,
    val dialogId: String,
    var renderedText: String = ""
)
