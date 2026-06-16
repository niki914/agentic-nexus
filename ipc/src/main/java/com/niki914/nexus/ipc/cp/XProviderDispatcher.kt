package com.niki914.nexus.ipc.cp

import android.content.Context
import android.os.Bundle
import com.niki914.nexus.ipc.IpcContract
import com.niki914.nexus.ipc.XNotificationBridge
import com.niki914.nexus.ipc.ipcBundleOf
import com.niki914.nexus.ipc.readString
import com.niki914.nexus.ipc.store.StoreDescriptorRegistry
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

            IpcContract.Method.GET_STORE -> {
                val storeId = extras?.readString(IpcContract.Field.STORE_ID)
                    ?: return null
                respondWithStoreHandle(storeId) ?: return null
            }

            IpcContract.Method.MUTATE_STORE -> {
                mutateStore(
                    context = context,
                    extras = extras
                ) ?: return null
                respondSuccess()
            }

            IpcContract.Method.PUT_WEB_SETTINGS -> null

            IpcContract.Method.MUTATE_WEB_SETTINGS -> {
                mutateStore(
                    context = context,
                    store = IpcContract.Store.WEB_SETTINGS,
                    extras = extras
                ) ?: return null
                respondSuccess()
            }

            IpcContract.Method.GET_LOCAL_SETTINGS -> respondWithStoreHandle(IpcContract.Store.LOCAL_SETTINGS)

            IpcContract.Method.PUT_LOCAL_SETTINGS -> null

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

    private fun respondWithStoreHandle(
        storeId: String
    ): Bundle? {
        StoreDescriptorRegistry.resolveDynamic(storeId) ?: return null
        return ipcBundleOf(
            IpcContract.Field.SUCCESS to true,
            IpcContract.Field.STORE_URI to IpcContract.storeFileUri(storeId).toString()
        )
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

    private fun mutateStore(
        context: Context,
        extras: Bundle?
    ): Unit? {
        val storeId = extras?.readString(IpcContract.Field.STORE_ID)
            ?: return null
        StoreDescriptorRegistry.resolveDynamic(storeId) ?: return null
        val path = extras.readString(IpcContract.Field.PATH)
            ?.takeIf(String::isNotBlank)
            ?: return null
        val valueJson = extras.readString(IpcContract.Field.VALUE_JSON)
            ?: return null
        return runCatching {
            runBlocking {
                XIpcStoreRepository.mutateJson(
                    context = context,
                    storeId = storeId,
                    path = path,
                    valueJson = valueJson
                )
            }
        }.fold(
            onSuccess = { Unit },
            onFailure = { null }
        )
    }

    private fun respondSuccess(): Bundle {
        return ipcBundleOf(IpcContract.Field.SUCCESS to true)
    }
}
