package com.niki914.nexus.agentic.app.ui.nexus.model

import com.niki914.nexus.agentic.runtime.settings.model.RuntimeLlmConfig as LlmConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConfigureViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun save_withIncompleteFields_staysOnPageAndRequestsFieldFocus() = runTest {
        var saveCalled = false
        val viewModel = ConfigureViewModel(
            loadLlmConfig = { LlmConfig() },
            saveLlmAccess = { _, _, _, _ -> saveCalled = true },
        )
        val effectDeferred = async { viewModel.uiEffect.first() }

        viewModel.sendIntent(ConfigureIntent.Initialize("deepseek"))
        advanceUntilIdle()
        viewModel.sendIntent(ConfigureIntent.Save)
        advanceUntilIdle()

        val state = viewModel.uiStateFlow.value
        assertFalse(saveCalled)
        assertFalse(state.isSaving)
        assertNull(state.inlineError)
        assertEquals(ConfigureEffect.FocusModel, effectDeferred.await())
    }

    @Test
    fun save_withCompleteFields_persistsSettings() = runTest {
        var savedConfig: LlmConfig? = null
        val viewModel = ConfigureViewModel(
            loadLlmConfig = { LlmConfig() },
            saveLlmAccess = { provider, endpoint, model, apiKey ->
                savedConfig = LlmConfig(
                    provider = provider,
                    endpoint = endpoint,
                    model = model,
                    apiKey = apiKey,
                )
            },
        )

        viewModel.sendIntent(ConfigureIntent.Initialize("deepseek"))
        advanceUntilIdle()
        viewModel.sendIntent(ConfigureIntent.UpdateModel("deepseek-chat"))
        viewModel.sendIntent(ConfigureIntent.UpdateApiKey("sk-demo"))
        viewModel.sendIntent(ConfigureIntent.Save)
        advanceUntilIdle()

        assertEquals("deepseek", savedConfig?.provider)
        assertEquals("deepseek-chat", savedConfig?.model)
        assertEquals("sk-demo", savedConfig?.apiKey)
        assertNull(viewModel.uiStateFlow.value.inlineError)
    }

    @Test
    fun endpointOverrideToggle_usesDefaultWhenDisabledAndRestoresCustomWhenEnabled() = runTest {
        var savedConfig: LlmConfig? = null
        val viewModel = ConfigureViewModel(
            loadLlmConfig = { LlmConfig() },
            saveLlmAccess = { provider, endpoint, model, apiKey ->
                savedConfig = LlmConfig(
                    provider = provider,
                    endpoint = endpoint,
                    model = model,
                    apiKey = apiKey,
                )
            },
        )
        val officialEndpoint = ProviderSpecs.find("openai").officialEndpoint

        viewModel.sendIntent(ConfigureIntent.Initialize("openai"))
        advanceUntilIdle()
        viewModel.sendIntent(ConfigureIntent.SetEndpointOverride(true))
        advanceUntilIdle()
        assertEquals(officialEndpoint, viewModel.uiStateFlow.value.endpointInput)

        viewModel.sendIntent(ConfigureIntent.UpdateEndpoint("abc"))
        viewModel.sendIntent(ConfigureIntent.UpdateModel("gpt-5.4"))
        viewModel.sendIntent(ConfigureIntent.UpdateApiKey("sk-demo"))
        advanceUntilIdle()
        viewModel.sendIntent(ConfigureIntent.SetEndpointOverride(false))
        advanceUntilIdle()

        assertEquals(officialEndpoint, viewModel.uiStateFlow.value.endpointInput)
        viewModel.sendIntent(ConfigureIntent.Save)
        advanceUntilIdle()
        assertEquals(officialEndpoint, savedConfig?.endpoint)

        viewModel.sendIntent(ConfigureIntent.SetEndpointOverride(true))
        advanceUntilIdle()

        assertEquals("abc", viewModel.uiStateFlow.value.endpointInput)
    }
}
