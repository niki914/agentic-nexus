package com.niki914.nexus.agentic.app.ui.nexus.content

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsConfigPageSourceTest {
    private val settingsDetailPageContentFile = File(
        "src/main/java/com/niki914/nexus/agentic/app/ui/nexus/content/SettingsDetailPageContent.kt",
    )
    private val configurePageContentFile = File(
        "src/main/java/com/niki914/nexus/agentic/app/ui/nexus/content/ConfigurePageContent.kt",
    )
    private val providerAccessSettingsBlockFile = File(
        "src/main/java/com/niki914/nexus/agentic/app/ui/nexus/content/ProviderAccessSettingsBlock.kt",
    )
    private val nexusPagesFile = File(
        "src/main/java/com/niki914/nexus/agentic/app/ui/nexus/NexusPages.kt",
    )

    @Test
    fun settings_detail_dispatches_provider_model_to_settings_config_page() {
        val source = settingsDetailPageContentFile.readText()

        assertSourceContains(source, "group == NexusSettingsGroup.ProviderModel")
        assertSourceContains(source, "ProviderModelSettingsContent(")
        assertSourceContains(source, "pageViewModel<ConfigureViewModel>(")
        assertSourceContains(source, "key = \"settings-configure\"")
        assertSourceContains(source, "ConfigureIntent.Initialize(scene = ConfigureScene.Settings)")
        assertSourceContains(source, "ConfigureEffect.SettingsSaveSucceeded -> onBack()")
        assertSourceContains(source, "ConfigureIntent.UpdatePrompt(prompt)")
        assertSourceContains(source, "ConfigureIntent.UpdateProxy(proxy)")
        assertSourceContains(source, "ConfigureEditableField.Proxy")
    }

    @Test
    fun configure_page_composes_advanced_settings_block_for_settings_scene() {
        val source = configurePageContentFile.readText()

        assertSourceContains(source, "configurePagePolicy(uiState.scene, uiState.providerSpec)")
        assertSourceContains(source, "ConfigureScene.Settings -> R.string.ui_settings_configure_save")
        assertSourceContains(source, "ConfigureScene.Settings -> R.string.ui_settings_configure_description")
        assertSourceContains(source, "if (policy.showAdvancedSection)")
        assertSourceContains(source, "ProviderAdvancedSettingsBlock(")
        assertSourceContains(source, "onPromptChange = onPromptChange")
        assertSourceContains(source, "onProxyChange = onProxyChange")
        assertSourceContains(source, "requestedFocusField")
    }

    @Test
    fun provider_access_block_uses_policy_for_endpoint_and_override_toggle() {
        val source = providerAccessSettingsBlockFile.readText()

        assertSourceContains(source, "if (policy.showEndpointSection)")
        assertSourceContains(source, "policy.endpointEditable")
        assertSourceContains(source, "if (policy.showEndpointOverrideToggle)")
        assertSourceContains(source, "SettingToggleItem(")
        assertSourceContains(source, "enabled = endpointEditable && !uiState.isSaving")
    }

    @Test
    fun nexus_pages_keeps_onboarding_effect_separate_from_settings_effect() {
        val source = nexusPagesFile.readText()

        assertSourceContains(source, "ConfigureIntent.Initialize(")
        assertSourceContains(source, "scene = ConfigureScene.Onboarding")
        assertSourceContains(source, "ConfigureEffect.OnboardingSaveSucceeded -> onPush(DonePage)")
        assertSourceContains(source, "ConfigureEffect.SettingsSaveSucceeded,")
        assertSourceContains(source, "ConfigureEffect.FocusProxy,")
        assertSourceContains(source, "onBack = onPop")
    }

    private fun assertSourceContains(source: String, expected: String) {
        assertTrue("缺失源码契约片段: $expected", source.contains(expected))
    }
}
