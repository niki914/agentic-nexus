package com.niki914.nexus.ipc.store

import android.content.Context
import com.niki914.nexus.ipc.IpcContract
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

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
            val updatedJson = updateJsonValue(
                json = readPersistedJson(context, store) ?: EMPTY_JSON,
                key = path,
                value = parseValueJson(valueJson)
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

    private fun updateJsonValue(
        json: String,
        key: String,
        value: Any?
    ): String {
        val root = parseJsonObject(json)
        val segments = key
            .split(".")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        require(segments.isNotEmpty()) { "key must not be blank" }

        var current = root
        segments.dropLast(1).forEach { segment ->
            val next = current.optJSONObject(segment) ?: JSONObject().also {
                current.put(segment, it)
            }
            current = next
        }
        current.put(segments.last(), toJsonValue(value))
        return root.toString()
    }

    private fun parseJsonObject(json: String): JSONObject {
        return runCatching {
            if (json.isBlank()) JSONObject() else JSONObject(json)
        }.getOrElse {
            JSONObject()
        }
    }

    private fun parseValueJson(valueJson: String): Any? {
        return runCatching {
            JSONTokener(valueJson).nextValue()
        }.getOrElse {
            valueJson
        }
    }

    private fun toJsonValue(value: Any?): Any {
        return when (value) {
            null -> JSONObject.NULL
            JSONObject.NULL -> JSONObject.NULL
            is JSONObject -> value
            is JSONArray -> value
            is Boolean, is Number, is String -> value
            is Map<*, *> -> JSONObject().apply {
                value.forEach { (key, nestedValue) ->
                    if (key != null) {
                        put(key.toString(), toJsonValue(nestedValue))
                    }
                }
            }
            is Iterable<*> -> JSONArray().apply {
                value.forEach { put(toJsonValue(it)) }
            }
            is Array<*> -> JSONArray().apply {
                value.forEach { put(toJsonValue(it)) }
            }
            else -> value.toString()
        }
    }

    private const val EMPTY_JSON = "{}"
}
