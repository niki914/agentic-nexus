package com.niki914.nexus.agentic.runtime.settings.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RuntimeSkillModelsTest {
    @Test
    fun runtimeSkillMetadata_exposesSkillIdentityPathAndEnabledState() {
        val metadata = RuntimeSkillMetadata(
            id = "group-a/skill-a",
            name = "Skill A",
            description = "Does one thing.",
            relativePath = "group-a/skill-a/SKILL.md",
            absolutePath = "/tmp/skills/group-a/skill-a/SKILL.md",
            enabled = true,
        )

        assertEquals("group-a/skill-a", metadata.id)
        assertEquals("Skill A", metadata.name)
        assertEquals("Does one thing.", metadata.description)
        assertEquals("group-a/skill-a/SKILL.md", metadata.relativePath)
        assertEquals("/tmp/skills/group-a/skill-a/SKILL.md", metadata.absolutePath)
        assertEquals(true, metadata.enabled)
    }

    @Test
    fun runtimeLoadedSkill_exposesMetadataAndContent() {
        val loaded = RuntimeLoadedSkill(
            id = "skill-a",
            name = "Skill A",
            description = "",
            relativePath = "skill-a/SKILL.md",
            absolutePath = "/tmp/skills/skill-a/SKILL.md",
            content = "# Skill A",
            enabled = false,
        )

        assertEquals("skill-a", loaded.id)
        assertEquals("Skill A", loaded.name)
        assertEquals("", loaded.description)
        assertEquals("skill-a/SKILL.md", loaded.relativePath)
        assertEquals("/tmp/skills/skill-a/SKILL.md", loaded.absolutePath)
        assertEquals("# Skill A", loaded.content)
        assertEquals(false, loaded.enabled)
    }

    @Test
    fun runtimeSkillValidation_exposesFieldAndMessage() {
        val validation = RuntimeSkillValidation(
            field = "id",
            message = "Invalid skill id.",
        )

        assertEquals("id", validation.field)
        assertEquals("Invalid skill id.", validation.message)
    }
}
