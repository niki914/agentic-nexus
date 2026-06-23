package com.niki914.nexus.agentic.repo

import com.niki914.nexus.agentic.runtime.settings.model.RuntimeLoadedSkill
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeSkillMetadata
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeSkillValidation
import java.io.File

class SkillFileRepository(
    private val skillsRoot: File,
) {
    private val resolver = SkillPathResolver(skillsRoot)
    private val parser = SkillFrontmatterParser()
    private val stateStore = SkillEnabledStateStore(File(skillsRoot, STATUS_FILE_NAME))

    fun listAll(): List<RuntimeSkillMetadata> {
        return resolver.scanSkillFiles().map(::metadataFor)
    }

    fun listEnabled(): List<RuntimeSkillMetadata> {
        return listAll().filter { it.enabled }
    }

    fun load(id: String): RuntimeLoadedSkill? {
        val resolved = resolver.resolveSkillFile(id) as? SkillPathResolution.Resolved ?: return null
        if (!resolved.skillFile.isFile) {
            return null
        }
        val content = resolved.skillFile.readText(Charsets.UTF_8)
        val metadata = parser.parse(resolved.id, content)
        return RuntimeLoadedSkill(
            id = resolved.id,
            name = metadata.name,
            description = metadata.description,
            relativePath = resolved.relativePath,
            absolutePath = resolved.skillFile.canonicalPath,
            absoluteDir = resolved.skillDir.canonicalPath,
            content = content,
            enabled = stateStore.isEnabled(resolved.id),
        )
    }

    fun saveContent(id: String, content: String): RuntimeSkillValidation? {
        val resolved = resolveExisting(id) ?: return validationForMissingOrInvalid(id)
        resolved.skillFile.writeText(content, Charsets.UTF_8)
        return null
    }

    fun setEnabled(id: String, enabled: Boolean): RuntimeSkillValidation? {
        val resolved = resolveExisting(id) ?: return validationForMissingOrInvalid(id)
        stateStore.setEnabled(resolved.id, enabled)
        return null
    }

    fun delete(id: String): RuntimeSkillValidation? {
        val resolved = resolveExisting(id) ?: return validationForMissingOrInvalid(id)
        if (!resolved.skillFile.delete() && resolved.skillFile.exists()) {
            return RuntimeSkillValidation("id", "Failed to delete skill.")
        }
        stateStore.remove(resolved.id)
        cleanEmptySkillDirs(resolved.skillDir)
        return null
    }

    private fun metadataFor(resolved: SkillPathResolution.Resolved): RuntimeSkillMetadata {
        val content = resolved.skillFile.readText(Charsets.UTF_8)
        val metadata = parser.parse(resolved.id, content)
        return RuntimeSkillMetadata(
            id = resolved.id,
            name = metadata.name,
            description = metadata.description,
            relativePath = resolved.relativePath,
            absolutePath = resolved.skillFile.canonicalPath,
            absoluteDir = resolved.skillDir.canonicalPath,
            enabled = stateStore.isEnabled(resolved.id),
        )
    }

    private fun resolveExisting(id: String): SkillPathResolution.Resolved? {
        val resolved = resolver.resolveSkillFile(id) as? SkillPathResolution.Resolved ?: return null
        return resolved.takeIf { it.skillFile.isFile }
    }

    private fun validationForMissingOrInvalid(id: String): RuntimeSkillValidation {
        val resolution = resolver.resolveSkillFile(id)
        return when (resolution) {
            is SkillPathResolution.Invalid -> resolution.validation
            is SkillPathResolution.Resolved -> RuntimeSkillValidation("id", "Skill not found.")
        }
    }

    private fun cleanEmptySkillDirs(startDir: File) {
        val root = skillsRoot.canonicalFile
        var current: File? = startDir.canonicalFile
        while (current != null && current.path != root.path && isUnderRoot(current, root)) {
            val files = current.listFiles()
            if (files == null || files.isNotEmpty()) {
                return
            }
            if (!current.delete()) {
                return
            }
            current = current.parentFile?.canonicalFile
        }
    }

    private fun isUnderRoot(file: File, root: File): Boolean {
        return file.path.startsWith(root.path + File.separator)
    }

    private companion object {
        const val STATUS_FILE_NAME = "status.json"
    }
}
