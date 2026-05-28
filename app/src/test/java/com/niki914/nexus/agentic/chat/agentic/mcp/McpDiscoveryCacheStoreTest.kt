package com.niki914.nexus.agentic.chat.agentic.mcp

import android.content.Context
import android.content.ContextWrapper
import com.niki914.nexus.agentic.mod.LocalSettings
import com.niki914.nexus.agentic.repo.LocalSettingsStore
import com.niki914.nexus.agentic.repo.McpServer
import com.niki914.nexus.agentic.repo.McpTool
import com.niki914.nexus.agentic.repo.XRepo
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class McpDiscoveryCacheStoreTest {
    private val context: Context = object : ContextWrapper(null) {
        override fun getApplicationContext(): Context = this
    }

    @After
    fun tearDown() {
        XRepo.resetForTest()
    }

    @Test
    fun onToolsDiscovered_persistsParsedToolsIntoXRepoCache() = runTest {
        val store = FakeLocalSettingsStore(LocalSettings())
        XRepo.installStoreForTest(store)
        XRepo.init(context)

        McpDiscoveryCacheStore().onToolsDiscovered(
            url = "http://127.0.0.1:51338/mcp",
            headers = mapOf("Authorization" to "Bearer token"),
            responseJson = """
                {
                  "jsonrpc": "2.0",
                  "id": 1,
                  "result": {
                    "tools": [
                      {
                        "name": "lookupSymbol",
                        "description": "Lookup symbol definition",
                        "inputSchema": {"type": "object"}
                      }
                    ]
                  }
                }
            """.trimIndent(),
        )

        assertEquals(1, store.writeCount)
        assertEquals(
            listOf(
                McpTool(
                    name = "lookupSymbol",
                    description = "Lookup symbol definition",
                    inputSchemaJson = """{"type":"object"}""",
                )
            ),
            XRepo.mcp.cachedTools(
                McpServer(
                    name = "aslocate",
                    url = "http://127.0.0.1:51338/mcp",
                    headers = mapOf("Authorization" to "Bearer token"),
                )
            ),
        )
    }

    private class FakeLocalSettingsStore(
        initialSettings: LocalSettings,
    ) : LocalSettingsStore {
        var settings: LocalSettings = initialSettings
            private set
        var writeCount: Int = 0
            private set

        override suspend fun read(context: Context): LocalSettings = settings

        override suspend fun write(context: Context, settings: LocalSettings) {
            this.settings = settings
            writeCount++
        }
    }
}
