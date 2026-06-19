package com.niki914.nexus.agentic.repo

import android.content.Context
import android.content.ContextWrapper
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class XRepoRuntimeGatewayTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val context: Context by lazy {
        val filesDir = temporaryFolder.newFolder()
        object : ContextWrapper(null) {
            override fun getFilesDir(): File = filesDir
            override fun getApplicationContext(): Context = this
            override fun getPackageName(): String = "com.niki914.nexus.agentic"
        }
    }

    @After
    fun tearDown() {
        XRepo.resetForTest()
    }

    @Test
    fun readLlmConfig_includesMemoriesFromMemoryStore() = runTest {
        val store = FakeDomainSettingsStore()
        XRepo.installStoreForTest(store)
        XRepo.init(context)

        XRepo.saveLlm(com.niki914.nexus.agentic.runtime.settings.model.RuntimeLlmConfig(
            provider = "openai",
            endpoint = "https://api.openai.com",
            model = "gpt-4",
            prompt = "Hello"
        ))
        
        XRepo.memory.replaceAll(listOf("Fact 1", "Fact 2"))

        val gateway = XRepoRuntimeGateway(XRepo)
        val config = gateway.readLlmConfig()

        assertEquals("openai", config.provider)
        assertEquals(listOf("Fact 1", "Fact 2"), config.memories)
    }
}
