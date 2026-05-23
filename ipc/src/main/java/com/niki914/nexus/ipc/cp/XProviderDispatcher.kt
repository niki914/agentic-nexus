package com.niki914.nexus.ipc.cp

import android.content.Context
import android.os.Bundle
import com.niki914.nexus.ipc.IpcContract
import com.niki914.nexus.ipc.XNotificationBridge
import com.niki914.nexus.ipc.ipcBundleOf
import com.niki914.nexus.ipc.readString
import com.niki914.nexus.ipc.store.XIpcStoreRepository
import kotlinx.coroutines.runBlocking

internal object XProviderDispatcher {

    fun dispatch(
        context: Context,
        method: IpcContract.Method,
        extras: Bundle?
    ): Bundle? {
        return when (method) {
            IpcContract.Method.GET_CONFIG,
            IpcContract.Method.GET_WEB_SETTINGS -> respondWithStoreHandle(IpcContract.Store.WEB_SETTINGS)

            IpcContract.Method.PUT_WEB_SETTINGS -> {
                val json = extras?.readString(IpcContract.Store.WEB_SETTINGS.payloadField)
                    ?: IpcContract.Store.WEB_SETTINGS.legacyPayloadField?.let { field ->
                        extras?.readString(field)
                    }
                    ?: return null
                writeStore(
                    context = context,
                    store = IpcContract.Store.WEB_SETTINGS,
                    json = json
                )
                respondSuccess()
            }

            IpcContract.Method.MUTATE_WEB_SETTINGS -> {
                mutateStore(
                    context = context,
                    store = IpcContract.Store.WEB_SETTINGS,
                    extras = extras
                ) ?: return null
                respondSuccess()
            }

            IpcContract.Method.GET_LOCAL_SETTINGS -> respondWithStoreHandle(IpcContract.Store.LOCAL_SETTINGS)

            IpcContract.Method.PUT_LOCAL_SETTINGS -> {
                val json = extras?.readString(IpcContract.Store.LOCAL_SETTINGS.payloadField)
                    ?: return null
                writeStore(
                    context = context,
                    store = IpcContract.Store.LOCAL_SETTINGS,
                    json = json
                )
                respondSuccess()
            }

            IpcContract.Method.MUTATE_LOCAL_SETTINGS -> {
                mutateStore(
                    context = context,
                    store = IpcContract.Store.LOCAL_SETTINGS,
                    extras = extras
                ) ?: return null
                respondSuccess()
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

    private fun respondWithStoreHandle(
        store: IpcContract.Store
    ): Bundle {
        return ipcBundleOf(
            IpcContract.Field.SUCCESS to true,
            IpcContract.Field.STORE_URI to store.fileUri.toString()
        )
    }

    private fun writeStore(
        context: Context,
        store: IpcContract.Store,
        json: String
    ) {
        runBlocking {
            XIpcStoreRepository.writeJson(context, store, json)
        }
    }

    private fun mutateStore(
        context: Context,
        store: IpcContract.Store,
        extras: Bundle?
    ): Unit? {
        val path = extras?.readString(IpcContract.Field.PATH)
            ?.takeIf(String::isNotBlank)
            ?: return null
        val valueJson = extras.readString(IpcContract.Field.VALUE_JSON)
            ?: return null
        runBlocking {
            XIpcStoreRepository.mutateJson(
                context = context,
                store = store,
                path = path,
                valueJson = valueJson
            )
        }
        return Unit
    }

    private fun respondSuccess(): Bundle {
        return ipcBundleOf(IpcContract.Field.SUCCESS to true)
    }
}
