package com.niki914.nexus.ipc

import android.content.Context
import android.os.Bundle
import kotlinx.coroutines.runBlocking

internal object XProviderDispatcher {

    fun dispatch(
        context: Context,
        method: IpcContract.Method,
        extras: Bundle?
    ): Bundle? {
        return when (method) {
            IpcContract.Method.GET_CONFIG,
            IpcContract.Method.GET_WEB_SETTINGS -> respondWithStore(
                context = context,
                store = IpcContract.Store.WEB_SETTINGS
            )

            IpcContract.Method.PUT_WEB_SETTINGS -> {
                val json = extras?.readString(IpcContract.Store.WEB_SETTINGS.payloadField)
                    ?: IpcContract.Store.WEB_SETTINGS.legacyPayloadField?.let { field ->
                        extras?.readString(field)
                    }
                    ?: return null
                respondWithWrittenStore(
                    context = context,
                    store = IpcContract.Store.WEB_SETTINGS,
                    json = json
                )
            }

            IpcContract.Method.MUTATE_WEB_SETTINGS -> {
                respondWithMutatedStore(
                    context = context,
                    store = IpcContract.Store.WEB_SETTINGS,
                    extras = extras
                )
            }

            IpcContract.Method.GET_LOCAL_SETTINGS -> respondWithStore(
                context = context,
                store = IpcContract.Store.LOCAL_SETTINGS
            )

            IpcContract.Method.PUT_LOCAL_SETTINGS -> {
                val json = extras?.readString(IpcContract.Store.LOCAL_SETTINGS.payloadField)
                    ?: return null
                respondWithWrittenStore(
                    context = context,
                    store = IpcContract.Store.LOCAL_SETTINGS,
                    json = json
                )
            }

            IpcContract.Method.MUTATE_LOCAL_SETTINGS -> {
                respondWithMutatedStore(
                    context = context,
                    store = IpcContract.Store.LOCAL_SETTINGS,
                    extras = extras
                )
            }

            IpcContract.Method.POST_NOTIFICATION -> {
                val title = extras?.readString(IpcContract.Field.TITLE).orEmpty()
                val content = extras?.readString(IpcContract.Field.CONTENT).orEmpty()
                val uri = extras?.readString(IpcContract.Field.URI)
                XNotificationBridge.post(context, title, content, uri)
                ipcBundleOf(IpcContract.Field.SUCCESS to true)
            }
        }
    }

    private fun respondWithStore(
        context: Context,
        store: IpcContract.Store
    ): Bundle {
        val json = runBlocking {
            XIpcStoreRepository.readJson(context, store)
        }
        return storeResponse(store, json)
    }

    private fun respondWithWrittenStore(
        context: Context,
        store: IpcContract.Store,
        json: String
    ): Bundle {
        val updatedJson = runBlocking {
            XIpcStoreRepository.writeJson(context, store, json)
        }
        return storeResponse(store, updatedJson)
    }

    private fun respondWithMutatedStore(
        context: Context,
        store: IpcContract.Store,
        extras: Bundle?
    ): Bundle? {
        val path = extras?.readString(IpcContract.Field.PATH)
            ?.takeIf(String::isNotBlank)
            ?: return null
        val valueJson = extras.readString(IpcContract.Field.VALUE_JSON)
            ?: return null
        val updatedJson = runBlocking {
            XIpcStoreRepository.mutateJson(
                context = context,
                store = store,
                path = path,
                valueJson = valueJson
            )
        }
        return storeResponse(store, updatedJson)
    }

    private fun storeResponse(
        store: IpcContract.Store,
        json: String
    ): Bundle {
        val pairs = buildList {
            add(IpcContract.Field.SUCCESS to true)
            add(store.payloadField to json)
            store.legacyPayloadField?.let { add(it to json) }
        }
        return ipcBundleOf(*pairs.toTypedArray())
    }
}
