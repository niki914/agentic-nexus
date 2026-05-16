package com.niki914.nexus.agentic.mod.feat.hyper

/** 单次流式渲染会话状态：记录当前 turnId、dialogId 和已累计文本，用于计算增量 delta。 */
data class XiaoaiRenderSession(
    val turnId: Long,
    val dialogId: String,
    var renderedText: String = ""
)
