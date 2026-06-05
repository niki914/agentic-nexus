package com.niki914.nexus.agentic.chat

import com.niki914.nexus.agentic.runtime.settings.model.RuntimeLlmConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class LLMControllerValidationTest {

    @Test
    fun validateLlmConfig_throwsWhenEndpointIsBlank() {
        val error = captureIllegalState {
            LLMController.validateLlmConfig(
                RuntimeLlmConfig(
                    endpoint = " ",
                    model = "deepseek-chat",
                )
            )
        }

        assertEquals(LLMController.CONFIG_REQUIRED_MESSAGE, error.message)
    }

    @Test
    fun validateLlmConfig_throwsWhenModelIsBlank() {
        val error = captureIllegalState {
            LLMController.validateLlmConfig(
                RuntimeLlmConfig(
                    endpoint = "https://example.com/v1",
                    model = "",
                )
            )
        }

        assertEquals(LLMController.CONFIG_REQUIRED_MESSAGE, error.message)
    }

    @Test
    fun validateLlmConfig_acceptsConfiguredEndpointAndModel() {
        LLMController.validateLlmConfig(
            RuntimeLlmConfig(
                endpoint = "https://example.com/v1",
                model = "deepseek-chat",
            )
        )
    }

    private fun captureIllegalState(block: () -> Unit): IllegalStateException {
        return try {
            block()
            fail("expected IllegalStateException")
            throw IllegalStateException("unreachable")
        } catch (error: IllegalStateException) {
            error
        }
    }
}
