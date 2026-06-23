package com.niki914.nexus.agentic.app.ui.nexus.model

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeLoadedSkill as LoadedSkill
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeSkillMetadata as SkillMetadata
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeSkillValidation as SkillValidation

@OptIn(ExperimentalCoroutinesApi::class)
class SkillSettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun load_withItems_updatesListState() = runTest {
        val provider = FakeSkillRepositoryProvider(
            listResult = listOf(
                skillMetadata(
                    id = "local/battery/SKILL.md",
                    name = "Battery",
                    description = "Read battery state",
                    enabled = true,
                ),
            ),
        )
        val viewModel = SkillSettingsViewModel(provider)

        viewModel.sendIntent(SkillSettingsIntent.Load)
        advanceUntilIdle()

        val state = viewModel.uiStateFlow.value
        assertFalse(state.isLoading)
        assertEquals(
            listOf(
                SkillListItem(
                    id = "local/battery/SKILL.md",
                    title = "Battery",
                    summary = "Read battery state",
                    enabled = true,
                ),
            ),
            state.items,
        )
        assertEquals(null, state.inlineError)
    }

    @Test
    fun load_withEmptyList_showsEmptyState() = runTest {
        val viewModel = SkillSettingsViewModel(FakeSkillRepositoryProvider(listResult = emptyList()))

        viewModel.sendIntent(SkillSettingsIntent.Load)
        advanceUntilIdle()

        val state = viewModel.uiStateFlow.value
        assertFalse(state.isLoading)
        assertTrue(state.items.isEmpty())
        assertEquals(null, state.inlineError)
    }

    @Test
    fun load_whenProviderThrows_setsLoadFailed() = runTest {
        val viewModel = SkillSettingsViewModel(
            FakeSkillRepositoryProvider(listThrowable = IllegalStateException("boom")),
        )

        viewModel.sendIntent(SkillSettingsIntent.Load)
        advanceUntilIdle()

        val state = viewModel.uiStateFlow.value
        assertFalse(state.isLoading)
        assertTrue(state.inlineError is SkillInlineError.LoadFailed)
        assertEquals("boom", (state.inlineError as SkillInlineError.LoadFailed).message)
    }

    @Test
    fun toggleEnabled_whenSaveSucceeds_keepsUpdatedState() = runTest {
        val provider = FakeSkillRepositoryProvider(
            listResult = listOf(skillMetadata(enabled = true)),
        )
        val viewModel = SkillSettingsViewModel(provider)

        viewModel.sendIntent(SkillSettingsIntent.Load)
        advanceUntilIdle()
        viewModel.sendIntent(SkillSettingsIntent.ToggleEnabled(id = "skill/SKILL.md", enabled = false))
        advanceUntilIdle()

        val state = viewModel.uiStateFlow.value
        assertFalse(state.isSaving)
        assertFalse(state.items.single().enabled)
        assertEquals("skill/SKILL.md" to false, provider.toggleCalls.single())
        assertEquals(null, state.inlineError)
    }

    @Test
    fun toggleEnabled_whenValidationFails_rollsBackAndShowsError() = runTest {
        val viewModel = SkillSettingsViewModel(
            FakeSkillRepositoryProvider(
                listResult = listOf(skillMetadata(enabled = true)),
                toggleValidation = validation("enabled", "invalid toggle"),
            ),
        )

        viewModel.sendIntent(SkillSettingsIntent.Load)
        advanceUntilIdle()
        viewModel.sendIntent(SkillSettingsIntent.ToggleEnabled(id = "skill/SKILL.md", enabled = false))
        advanceUntilIdle()

        val state = viewModel.uiStateFlow.value
        assertFalse(state.isSaving)
        assertTrue(state.items.single().enabled)
        assertTrue(state.inlineError is SkillInlineError.SaveFailed)
        assertEquals("invalid toggle", (state.inlineError as SkillInlineError.SaveFailed).message)
    }

    @Test
    fun toggleEnabled_whenProviderThrows_rollsBackAndShowsError() = runTest {
        val viewModel = SkillSettingsViewModel(
            FakeSkillRepositoryProvider(
                listResult = listOf(skillMetadata(enabled = true)),
                toggleThrowable = IllegalStateException("toggle crash"),
            ),
        )

        viewModel.sendIntent(SkillSettingsIntent.Load)
        advanceUntilIdle()
        viewModel.sendIntent(SkillSettingsIntent.ToggleEnabled(id = "skill/SKILL.md", enabled = false))
        advanceUntilIdle()

        val state = viewModel.uiStateFlow.value
        assertFalse(state.isSaving)
        assertTrue(state.items.single().enabled)
        assertTrue(state.inlineError is SkillInlineError.SaveFailed)
        assertEquals("toggle crash", (state.inlineError as SkillInlineError.SaveFailed).message)
    }

    @Test
    fun loadDetail_withContent_updatesFormSnapshot() = runTest {
        val viewModel = SkillSettingsViewModel(
            FakeSkillRepositoryProvider(detailResult = loadedSkill(name = "Loaded", content = "# Skill")),
        )

        viewModel.sendIntent(SkillSettingsIntent.LoadDetail(id = "skill/SKILL.md", fallbackTitle = "Fallback"))
        advanceUntilIdle()

        val formState = viewModel.uiStateFlow.value.formState
        assertFalse(viewModel.uiStateFlow.value.isLoading)
        assertEquals("skill/SKILL.md", formState.skillId)
        assertEquals("Loaded", formState.title)
        assertEquals("# Skill", formState.content)
        assertEquals("# Skill", formState.initialContent)
        assertFalse(formState.hasUnsavedChanges)
    }

    @Test
    fun loadDetail_whenMissing_setsLoadFailed() = runTest {
        val viewModel = SkillSettingsViewModel(FakeSkillRepositoryProvider(detailResult = null))

        viewModel.sendIntent(SkillSettingsIntent.LoadDetail(id = "missing/SKILL.md", fallbackTitle = "Missing"))
        advanceUntilIdle()

        val state = viewModel.uiStateFlow.value
        assertFalse(state.isLoading)
        assertTrue(state.inlineError is SkillInlineError.LoadFailed)
    }

    @Test
    fun contentChanged_marksUnsavedChanges() = runTest {
        val viewModel = SkillSettingsViewModel(
            FakeSkillRepositoryProvider(detailResult = loadedSkill(content = "initial")),
        )

        viewModel.sendIntent(SkillSettingsIntent.LoadDetail(id = "skill/SKILL.md", fallbackTitle = "Skill"))
        advanceUntilIdle()
        viewModel.sendIntent(SkillSettingsIntent.ContentChanged("edited"))
        advanceUntilIdle()

        val formState = viewModel.uiStateFlow.value.formState
        assertEquals("edited", formState.content)
        assertEquals("initial", formState.initialContent)
        assertTrue(formState.hasUnsavedChanges)
    }

    @Test
    fun save_whenSucceeds_updatesInitialSnapshotAndStays() = runTest {
        val provider = FakeSkillRepositoryProvider(detailResult = loadedSkill(content = "initial"))
        val viewModel = SkillSettingsViewModel(provider)

        viewModel.sendIntent(SkillSettingsIntent.LoadDetail(id = "skill/SKILL.md", fallbackTitle = "Skill"))
        advanceUntilIdle()
        viewModel.sendIntent(SkillSettingsIntent.ContentChanged("edited"))
        viewModel.sendIntent(SkillSettingsIntent.Save)
        advanceUntilIdle()

        val state = viewModel.uiStateFlow.value
        assertFalse(state.isSaving)
        assertEquals("edited", state.formState.content)
        assertEquals("edited", state.formState.initialContent)
        assertFalse(state.formState.hasUnsavedChanges)
        assertEquals("skill/SKILL.md" to "edited", provider.saveCalls.single())
        assertEquals(null, state.inlineError)
    }

    @Test
    fun save_whenValidationFails_preservesEditedContent() = runTest {
        val viewModel = SkillSettingsViewModel(
            FakeSkillRepositoryProvider(
                detailResult = loadedSkill(content = "initial"),
                saveValidation = validation("content", "bad content"),
            ),
        )

        viewModel.sendIntent(SkillSettingsIntent.LoadDetail(id = "skill/SKILL.md", fallbackTitle = "Skill"))
        advanceUntilIdle()
        viewModel.sendIntent(SkillSettingsIntent.ContentChanged("edited"))
        viewModel.sendIntent(SkillSettingsIntent.Save)
        advanceUntilIdle()

        val state = viewModel.uiStateFlow.value
        assertFalse(state.isSaving)
        assertEquals("edited", state.formState.content)
        assertEquals("initial", state.formState.initialContent)
        assertTrue(state.formState.hasUnsavedChanges)
        assertTrue(state.inlineError is SkillInlineError.SaveFailed)
        assertEquals("bad content", (state.inlineError as SkillInlineError.SaveFailed).message)
    }

    @Test
    fun requestDelete_onlyShowsConfirmation() = runTest {
        val provider = FakeSkillRepositoryProvider(detailResult = loadedSkill(name = "Delete Me"))
        val viewModel = SkillSettingsViewModel(provider)

        viewModel.sendIntent(SkillSettingsIntent.LoadDetail(id = "skill/SKILL.md", fallbackTitle = "Skill"))
        advanceUntilIdle()
        viewModel.sendIntent(SkillSettingsIntent.RequestDelete)
        advanceUntilIdle()

        assertEquals(
            SkillDeleteConfirmationState(skillId = "skill/SKILL.md", title = "Delete Me"),
            viewModel.uiStateFlow.value.deleteConfirmation,
        )
        assertTrue(provider.deleteCalls.isEmpty())
    }

    @Test
    fun confirmDelete_whenSucceeds_emitsExitDetail() = runTest {
        val provider = FakeSkillRepositoryProvider(detailResult = loadedSkill())
        val viewModel = SkillSettingsViewModel(provider)
        val effects = collectEffects(viewModel, count = 1)

        viewModel.sendIntent(SkillSettingsIntent.LoadDetail(id = "skill/SKILL.md", fallbackTitle = "Skill"))
        advanceUntilIdle()
        viewModel.sendIntent(SkillSettingsIntent.RequestDelete)
        viewModel.sendIntent(SkillSettingsIntent.ConfirmDelete)
        advanceUntilIdle()

        val state = viewModel.uiStateFlow.value
        assertFalse(state.isSaving)
        assertEquals(null, state.deleteConfirmation)
        assertEquals(listOf("skill/SKILL.md"), provider.deleteCalls)
        assertEquals(listOf(SkillSettingsEffect.ExitDetail), effects)
    }

    @Test
    fun confirmDelete_whenFails_keepsUserInDetail() = runTest {
        val provider = FakeSkillRepositoryProvider(
            detailResult = loadedSkill(),
            deleteValidation = validation("id", "cannot delete"),
        )
        val viewModel = SkillSettingsViewModel(provider)
        val effects = collectEffects(viewModel, count = 1)

        viewModel.sendIntent(SkillSettingsIntent.LoadDetail(id = "skill/SKILL.md", fallbackTitle = "Skill"))
        advanceUntilIdle()
        viewModel.sendIntent(SkillSettingsIntent.RequestDelete)
        viewModel.sendIntent(SkillSettingsIntent.ConfirmDelete)
        advanceUntilIdle()

        val state = viewModel.uiStateFlow.value
        assertFalse(state.isSaving)
        assertEquals(null, state.deleteConfirmation)
        assertTrue(state.inlineError is SkillInlineError.DeleteFailed)
        assertEquals("cannot delete", (state.inlineError as SkillInlineError.DeleteFailed).message)
        assertTrue(effects.isEmpty())
    }

    private fun kotlinx.coroutines.test.TestScope.collectEffects(
        viewModel: SkillSettingsViewModel,
        count: Int,
    ): MutableList<SkillSettingsEffect> {
        val effects = mutableListOf<SkillSettingsEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEffect.take(count).toList(effects)
        }
        return effects
    }

    private class FakeSkillRepositoryProvider(
        private val listResult: List<SkillMetadata> = emptyList(),
        private val detailResult: LoadedSkill? = loadedSkill(),
        private val listThrowable: Throwable? = null,
        private val toggleValidation: SkillValidation? = null,
        private val toggleThrowable: Throwable? = null,
        private val saveValidation: SkillValidation? = null,
        private val saveThrowable: Throwable? = null,
        private val deleteValidation: SkillValidation? = null,
        private val deleteThrowable: Throwable? = null,
    ) : SkillRepositoryProvider {
        val toggleCalls = mutableListOf<Pair<String, Boolean>>()
        val saveCalls = mutableListOf<Pair<String, String>>()
        val deleteCalls = mutableListOf<String>()

        override suspend fun listAll(): List<SkillMetadata> {
            listThrowable?.let { throw it }
            return listResult
        }

        override suspend fun getDetail(id: String): LoadedSkill? {
            return detailResult
        }

        override suspend fun saveContent(id: String, content: String): SkillValidation? {
            saveThrowable?.let { throw it }
            saveCalls += id to content
            return saveValidation
        }

        override suspend fun setEnabled(id: String, enabled: Boolean): SkillValidation? {
            toggleThrowable?.let { throw it }
            toggleCalls += id to enabled
            return toggleValidation
        }

        override suspend fun delete(id: String): SkillValidation? {
            deleteThrowable?.let { throw it }
            deleteCalls += id
            return deleteValidation
        }
    }

    private companion object {
        fun skillMetadata(
            id: String = "skill/SKILL.md",
            name: String = "Skill",
            description: String = "Skill description",
            relativePath: String = "skill/SKILL.md",
            enabled: Boolean = true,
        ): SkillMetadata {
            return SkillMetadata(
                id = id,
                name = name,
                description = description,
                relativePath = relativePath,
                absolutePath = "/tmp/$relativePath",
                enabled = enabled,
            )
        }

        fun loadedSkill(
            id: String = "skill/SKILL.md",
            name: String = "Skill",
            description: String = "Skill description",
            relativePath: String = "skill/SKILL.md",
            content: String = "# Skill",
            enabled: Boolean = true,
        ): LoadedSkill {
            return LoadedSkill(
                id = id,
                name = name,
                description = description,
                relativePath = relativePath,
                absolutePath = "/tmp/$relativePath",
                content = content,
                enabled = enabled,
            )
        }

        fun validation(field: String, message: String): SkillValidation {
            return SkillValidation(field = field, message = message)
        }
    }
}
