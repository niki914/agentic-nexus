package com.niki914.nexus.agentic.repo

import com.niki914.nexus.agentic.runtime.settings.model.RuntimeLoadedSkill
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeSkillMetadata
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeSkillValidation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class SkillApi internal constructor(
    private val repo: XRepo,
) {
    suspend fun listAll(): List<RuntimeSkillMetadata> {
        return repository().listAll()
    }

    suspend fun listEnabled(): List<RuntimeSkillMetadata> {
        return repository().listEnabled()
    }

    suspend fun getDetail(id: String): RuntimeLoadedSkill? {
        return repository().load(id)
    }

    suspend fun saveContent(id: String, content: String): RuntimeSkillValidation? {
        return repository().saveContent(id, content)
    }

    suspend fun setEnabled(id: String, enabled: Boolean): RuntimeSkillValidation? {
        return repository().setEnabled(id, enabled)
    }

    suspend fun delete(id: String): RuntimeSkillValidation? {
        return repository().delete(id)
    }

    suspend fun importSkill(sourceDir: File, overwrite: Boolean = false): SkillImportResult {
        return withContext(Dispatchers.IO) {
            repository().importSkill(sourceDir, overwrite)
        }
    }

    private suspend fun repository(): SkillFileRepository {
        val context = repo.context()
        return SkillFileRepository(File(context.filesDir, SKILLS_DIR_NAME))
    }

    private companion object {
        const val SKILLS_DIR_NAME = "skills"
    }
}
