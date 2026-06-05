package com.niki914.nexus.agentic.app.ui.nexus.content

import org.junit.Assert.assertEquals
import org.junit.Test

class AssistantErrorUiFormatterTest {

    @Test
    fun toAssistantErrorUi_formatsConfigRequiredMessage() {
        assertEquals(
            AssistantErrorUi(
                title = "配置还没填好",
                body = "请先填写模型地址和模型名称。",
            ),
            toAssistantErrorUi("请先填写配置"),
        )
    }

    @Test
    fun toAssistantErrorUi_usesGenericTitleForOtherErrors() {
        assertEquals(
            AssistantErrorUi(
                title = "当前无法继续",
                body = "network failed",
            ),
            toAssistantErrorUi("network failed"),
        )
    }

    @Test
    fun toAssistantErrorUi_fallsBackForBlankMessage() {
        assertEquals(
            AssistantErrorUi(
                title = "当前无法继续",
                body = "请稍后重试。",
            ),
            toAssistantErrorUi(" "),
        )
    }
}
