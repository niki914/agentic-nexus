package com.niki914.nexus.agentic.chat.agentic.mcp

import com.niki914.nexus.h.util.xTry
import com.niki914.nexus.h.util.xlog
import com.niki914.s3ss10n.net.HttpEngine
import com.niki914.s3ss10n.net.HttpRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

class McpInterceptorHttpEngine(
    private val delegate: HttpEngine,
    private val onToolsDiscovered: suspend (url: String, headers: Map<String, String>, responseJson: String) -> Unit,
) : HttpEngine {
    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }

    override fun stream(request: HttpRequest): Flow<String> = delegate.stream(request)

    override suspend fun unary(request: HttpRequest): String {
        val response = delegate.unary(request)
        if (request.isToolsListRequest()) {
            try {
                onToolsDiscovered(
                    request.url,
                    request.headers.filterKeys { !it.equals("Content-Type", ignoreCase = true) },
                    response
                )
            } catch (error: Throwable) {
                xlog("McpInterceptorHttpEngine.onToolsDiscovered failed for ${request.url}: ${error.message}")
            }
        }
        return response
    }

    override fun close() {
        delegate.close()
    }

    private fun HttpRequest.isToolsListRequest(): Boolean {
        if (method != "POST") {
            return false
        }
        val bodyBytes = body ?: return false
        val bodyText = bodyBytes.toString(Charsets.UTF_8)
        val root = xTry("McpInterceptorHttpEngine.isToolsListRequest:${url}") {
            json.parseToJsonElement(bodyText) as? JsonObject
        } ?: return false
        return root["method"]?.jsonPrimitive?.contentOrNull == "tools/list"
    }
}
