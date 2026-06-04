package com.niki914.nexus.agentic.app.ui.nexus.model

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AboutSettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun openItem_withGithub_emitsOpenUri() = runTest {
        val viewModel = AboutSettingsViewModel()
        val effectDeferred = async { viewModel.uiEffect.first() }

        viewModel.sendIntent(AboutSettingsIntent.OpenItem(AboutSettingsItemId.Github))
        advanceUntilIdle()

        assertEquals(
            AboutSettingsEffect.OpenUri("https://github.com/niki914/nexus"),
            effectDeferred.await(),
        )
    }

    @Test
    fun openItem_withFeatureFeedback_emitsEncodedIssueUri() = runTest {
        val viewModel = AboutSettingsViewModel()
        val effectDeferred = async { viewModel.uiEffect.first() }

        viewModel.sendIntent(AboutSettingsIntent.OpenItem(AboutSettingsItemId.FeatureFeedback))
        advanceUntilIdle()

        val uri = (effectDeferred.await() as AboutSettingsEffect.OpenUri).uri
        assertTrue(uri.startsWith("https://github.com/niki914/nexus/issues/new?"))
        assertTrue(uri.contains("title=%5BFEATURE%5D%20"))
        assertTrue(uri.contains("body="))
        assertFalse(uri.contains("## 功能建议"))
        assertFalse(uri.contains("\n"))
    }

    @Test
    fun openItem_withBugFeedback_emitsEncodedIssueUri() = runTest {
        val viewModel = AboutSettingsViewModel()
        val effectDeferred = async { viewModel.uiEffect.first() }

        viewModel.sendIntent(AboutSettingsIntent.OpenItem(AboutSettingsItemId.BugFeedback))
        advanceUntilIdle()

        val uri = (effectDeferred.await() as AboutSettingsEffect.OpenUri).uri
        assertTrue(uri.startsWith("https://github.com/niki914/nexus/issues/new?"))
        assertTrue(uri.contains("title=%5BBUG%5D%20"))
        assertTrue(uri.contains("body="))
        assertFalse(uri.contains("## 问题描述"))
        assertFalse(uri.contains("\n"))
    }
}
