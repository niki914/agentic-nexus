package com.niki914.nexus.agentic.app.ui.nexus.model

import com.niki914.nexus.agentic.mod.LocalSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
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
            loadSettings = { LocalSettings(JsonObject(emptyMap())) },
            saveSettings = { saveCalled = true },
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
        var savedSettings: LocalSettings? = null
        val viewModel = ConfigureViewModel(
            loadSettings = { LocalSettings(JsonObject(emptyMap())) },
            saveSettings = { settings -> savedSettings = settings },
        )

        viewModel.sendIntent(ConfigureIntent.Initialize("deepseek"))
        advanceUntilIdle()
        viewModel.sendIntent(ConfigureIntent.UpdateModel("deepseek-chat"))
        viewModel.sendIntent(ConfigureIntent.UpdateApiKey("sk-demo"))
        viewModel.sendIntent(ConfigureIntent.Save)
        advanceUntilIdle()

        assertEquals("deepseek", savedSettings?.provider)
        assertEquals("deepseek-chat", savedSettings?.model)
        assertEquals("sk-demo", savedSettings?.apiKey)
        assertNull(viewModel.uiStateFlow.value.inlineError)
    }

    @Test
    fun endpointOverrideToggle_usesDefaultWhenDisabledAndRestoresCustomWhenEnabled() = runTest {
        var savedSettings: LocalSettings? = null
        val viewModel = ConfigureViewModel(
            loadSettings = { LocalSettings(JsonObject(emptyMap())) },
            saveSettings = { settings -> savedSettings = settings },
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
        assertEquals(officialEndpoint, savedSettings?.endpoint)

        viewModel.sendIntent(ConfigureIntent.SetEndpointOverride(true))
        advanceUntilIdle()

        assertEquals("abc", viewModel.uiStateFlow.value.endpointInput)
    }
}
