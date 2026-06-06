package com.niki914.nexus.agentic.app.ui.nexus.content

import com.niki914.nexus.agentic.app.R
import com.niki914.nexus.agentic.chat.LlmErrorCode
import org.junit.Assert.assertEquals
import org.junit.Test

class AssistantErrorUiFormatterTest {

    @Test
    fun toAssistantErrorUi_formatsConfigRequiredMessage() {
        assertEquals(
            AssistantErrorUi(
                titleRes = R.string.ui_home_error_config_required_title,
                bodyRes = R.string.ui_home_error_config_required_body,
            ),
            toAssistantErrorUi("请先填写配置", LlmErrorCode.ConfigRequired),
        )
    }

    @Test
    fun toAssistantErrorUi_usesGenericTitleForOtherErrors() {
        assertEquals(
            AssistantErrorUi(
                titleRes = R.string.ui_home_error_generic_title,
                body = "network failed",
            ),
            toAssistantErrorUi("network failed"),
        )
    }

    @Test
    fun toAssistantErrorUi_fallsBackForBlankMessage() {
        assertEquals(
            AssistantErrorUi(
                titleRes = R.string.ui_home_error_generic_title,
                bodyRes = R.string.ui_home_error_retry_body,
            ),
            toAssistantErrorUi(" "),
        )
    }
}
