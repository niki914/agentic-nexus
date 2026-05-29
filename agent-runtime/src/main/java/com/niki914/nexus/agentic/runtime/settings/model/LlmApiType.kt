package com.niki914.nexus.agentic.runtime.settings.model

enum class LlmApiType {
    OpenAI,
    Anthropic,
    DeepSeek;

    companion object {
        fun fromProvider(provider: String): LlmApiType {
            return when (provider.trim().lowercase()) {
                "anthropic" -> Anthropic
                "deepseek" -> DeepSeek
                else -> OpenAI
            }
        }
    }
}
