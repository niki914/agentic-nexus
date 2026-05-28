package com.niki914.nexus.ipc.store

import android.content.Context
import com.niki914.nexus.ipc.IpcContract
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal object XIpcStoreRepository {

    private val webSettingsMutex = Mutex()
    private val localSettingsMutex = Mutex()

    suspend fun readJson(context: Context, store: IpcContract.Store): String {
        return mutexFor(store).withLock {
            readPersistedJson(context, store) ?: EMPTY_JSON
        }
    }

    suspend fun writeJson(
        context: Context,
        store: IpcContract.Store,
        json: String
    ): String {
        return mutexFor(store).withLock {
            writePersistedJson(context, store, json)
            json
        }
    }

    suspend fun mutateJson(
        context: Context,
        store: IpcContract.Store,
        path: String,
        valueJson: String
    ): String {
        return mutexFor(store).withLock {
            val updatedJson = IpcJsonMutator.mutate(
                json = readPersistedJson(context, store) ?: EMPTY_JSON,
                path = path,
                valueJson = valueJson
            )
            writePersistedJson(context, store, updatedJson)
            updatedJson
        }
    }

    private fun mutexFor(store: IpcContract.Store): Mutex {
        return when (store) {
            IpcContract.Store.WEB_SETTINGS -> webSettingsMutex
            IpcContract.Store.LOCAL_SETTINGS -> localSettingsMutex
        }
    }

    private fun readPersistedJson(
        context: Context,
        store: IpcContract.Store
    ): String? {
        return ConfigPersistence.readJson(context, store)
    }

    private fun writePersistedJson(
        context: Context,
        store: IpcContract.Store,
        json: String
    ) {
        ConfigPersistence.writeJson(context, store, json)
    }

    private const val EMPTY_JSON = "{}"
}
