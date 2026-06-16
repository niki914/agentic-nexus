package com.niki914.nexus.agentic.repo

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import com.niki914.nexus.agentic.repo.SettingsJsonCodecUtils.array
import com.niki914.nexus.agentic.repo.SettingsJsonCodecUtils.parseObject
import com.niki914.nexus.agentic.repo.SettingsJsonCodecUtils.string
import com.niki914.nexus.agentic.repo.SettingsJsonCodecUtils.stringValues
import com.niki914.nexus.agentic.runtime.settings.model.RuntimeLlmConfig as LlmConfig

internal object AgentSettingsCodec {
    fun parseMainConfig(json: String): LlmConfig {
        val llm = parseObject(json).obj(LLM_KEY) ?: return LlmConfig()
        return LlmConfig(
            provider = llm.string(PROVIDER_KEY),
            endpoint = llm.string(ENDPOINT_KEY),
            apiKey = llm.string(API_KEY_KEY),
            model = llm.string(MODEL_KEY),
            prompt = llm.string(PROMPT_KEY),
            proxy = llm.string(PROXY_KEY),
            memoryPrompt = llm.string(MEMORY_PROMPT_KEY),
            takeoverKeywords = llm.array(TAKEOVER_KEYWORDS_KEY).stringValues(),
        )
    }

    fun encodeMainConfig(config: LlmConfig): String {
        return JsonObject(
            mapOf(
                ID_KEY to JsonPrimitive(MAIN_AGENT_ID),
                LLM_KEY to JsonObject(
                    mapOf(
                        PROVIDER_KEY to JsonPrimitive(config.provider),
                        ENDPOINT_KEY to JsonPrimitive(config.endpoint),
                        MODEL_KEY to JsonPrimitive(config.model),
                        API_KEY_KEY to JsonPrimitive(config.apiKey),
                        PROMPT_KEY to JsonPrimitive(config.prompt),
                        PROXY_KEY to JsonPrimitive(config.proxy),
                        MEMORY_PROMPT_KEY to JsonPrimitive(config.memoryPrompt),
                        TAKEOVER_KEYWORDS_KEY to JsonArray(config.takeoverKeywords.map(::JsonPrimitive)),
                    )
                ),
            )
        ).toString()
    }

    private fun JsonObject.obj(key: String): JsonObject? {
        return this[key] as? JsonObject
    }

    private const val MAIN_AGENT_ID = "main"
    private const val ID_KEY = "id"
    private const val LLM_KEY = "llm"
    private const val PROVIDER_KEY = "provider"
    private const val ENDPOINT_KEY = "endpoint"
    private const val API_KEY_KEY = "api_key"
    private const val MODEL_KEY = "model"
    private const val PROMPT_KEY = "prompt"
    private const val PROXY_KEY = "proxy"
    private const val MEMORY_PROMPT_KEY = "memory_prompt"
    private const val TAKEOVER_KEYWORDS_KEY = "takeover_keywords"
}
