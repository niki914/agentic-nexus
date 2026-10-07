package com.niki914.zafiro.repo

import com.niki914.zafiro.repo.SettingsJsonCodecUtils.array
import com.niki914.zafiro.repo.SettingsJsonCodecUtils.boolean
import com.niki914.zafiro.repo.SettingsJsonCodecUtils.orEmptyObjects
import com.niki914.zafiro.repo.SettingsJsonCodecUtils.parseObject
import com.niki914.zafiro.repo.SettingsJsonCodecUtils.string
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal object TextActionsCodec {
    private const val ACTIONS_KEY = "actions"
    private const val ID_KEY = "id"
    private const val NAME_KEY = "name"
    private const val PROMPT_KEY = "prompt"
    private const val ENABLED_KEY = "enabled"

    fun parse(json: String): List<TextAction> {
        return parseObject(json)
            .array(ACTIONS_KEY)
            .orEmptyObjects()
            .mapNotNull { obj ->
                val id = obj.string(ID_KEY).trim()
                val name = obj.string(NAME_KEY).trim()
                val prompt = obj.string(PROMPT_KEY)
                if (id.isBlank() || name.isBlank() || prompt.isBlank()) return@mapNotNull null
                TextAction(
                    id = id,
                    name = name,
                    promptTemplate = prompt,
                    enabled = obj.boolean(ENABLED_KEY, default = true),
                )
            }
    }

    fun encode(actions: List<TextAction>): String {
        return JsonObject(
            mapOf(
                ACTIONS_KEY to JsonArray(
                    actions.map { action ->
                        JsonObject(
                            mapOf(
                                ID_KEY to JsonPrimitive(action.id),
                                NAME_KEY to JsonPrimitive(action.name),
                                PROMPT_KEY to JsonPrimitive(action.promptTemplate),
                                ENABLED_KEY to JsonPrimitive(action.enabled),
                            )
                        )
                    }
                )
            )
        ).toString()
    }
}
