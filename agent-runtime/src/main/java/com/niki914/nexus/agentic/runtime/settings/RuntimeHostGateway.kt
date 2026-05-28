package com.niki914.nexus.agentic.runtime.settings

interface RuntimeHostGateway {
    suspend fun postNotification(title: String, content: String, uri: String?): Boolean
}
