package com.niki914.nexus.agentic.repo

import android.content.Context
import android.content.ContextWrapper
import com.niki914.nexus.agentic.mod.LocalSettings
import com.niki914.nexus.ipc.store.StoreDescriptorRegistry
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DomainSettingsStoreTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun readMissingStoreReturnsDescriptorDefaultJson() = runTest {
        val context = testContext(packageName = "com.niki914.nexus.agentic")

        val json = XIpcDomainSettingsStore.readJson(context, StoreDescriptorRegistry.AGENT_MAIN_MEMORY_ID)

        assertEquals("""{"memories":[]}""", json)
    }

    @Test
    fun legacyDomainStoreOwnerWriteSuccessReturnsTrue() = runTest {
        val context = testContext(packageName = "com.niki914.nexus.agentic")
        val legacy = FakeLocalSettingsStore(LocalSettings())
        val store = LegacyLocalSettingsDomainStore(legacy)

        val success = store.writeJsonFromOwner(
            context,
            StoreDescriptorRegistry.APP_STATE_ID,
            """{"onboarding_completed":true}"""
        )

        assertTrue(success)
        assertTrue(legacy.settings.onboardingCompleted)
    }

    @Test
    fun hostFullWriteReturnsFalseWithoutCreatingOwnerFile() = runTest {
        val context = testContext(packageName = "com.heytap.speechassist")

        val success = XIpcDomainSettingsStore.writeJsonFromOwner(
            context,
            StoreDescriptorRegistry.APP_STATE_ID,
            """{"onboarding_completed":true}"""
        )

        assertFalse(success)
        assertFalse(storeFile(context, StoreDescriptorRegistry.APP_STATE_ID).exists())
    }

    @Test
    fun legacyDomainStoreUnknownMcpCacheReadReturnsDefaultJson() = runTest {
        val context = testContext(packageName = "com.niki914.nexus.agentic")
        val store = LegacyLocalSettingsDomainStore(FakeLocalSettingsStore(LocalSettings()))

        val json = store.readJson(
            context,
            StoreDescriptorRegistry.mcpCacheStoreId("missing_server")!!
        )

        assertEquals("{}", json)
    }

    private fun testContext(packageName: String): Context {
        val filesDir = temporaryFolder.newFolder()
        return object : ContextWrapper(null) {
            override fun getFilesDir(): File = filesDir
            override fun getApplicationContext(): Context = this
            override fun getPackageName(): String = packageName
        }
    }

    private fun storeFile(context: Context, storeId: String): File {
        val descriptor = StoreDescriptorRegistry.require(storeId)
        return File(context.filesDir, descriptor.relativePath)
    }

    private class FakeLocalSettingsStore(
        initialSettings: LocalSettings,
    ) : LocalSettingsStore {
        var settings: LocalSettings = initialSettings
            private set

        override suspend fun read(context: Context): LocalSettings = settings

        override suspend fun write(context: Context, settings: LocalSettings) {
            this.settings = settings
        }
    }
}
