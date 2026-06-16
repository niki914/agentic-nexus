package com.niki914.nexus.ipc.cp

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.Process
import com.niki914.nexus.ipc.IpcContract
import com.niki914.nexus.ipc.XValues
import com.niki914.nexus.ipc.store.ConfigPersistence
import com.niki914.nexus.ipc.store.StoreDescriptorRegistry
import com.niki914.nexus.ipc.store.XIpcStoreRepository
import java.io.FileInputStream
import kotlin.concurrent.thread
import kotlinx.coroutines.runBlocking

class SettingsContentProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        val appContext = context ?: return null
        if (!isCallerAllowed(appContext)) return null

        val resolvedMethod = IpcContract.Method.fromWire(method)
            ?: return super.call(method, arg, extras)
        return XProviderDispatcher.dispatch(
            context = appContext,
            method = resolvedMethod,
            extras = extras
        ) ?: super.call(method, arg, extras)
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val appContext = context ?: return null
        if (!isCallerAllowed(appContext)) return null
        val storeId = IpcContract.storeIdFromPathSegments(uri.pathSegments) ?: return null
        val descriptor = StoreDescriptorRegistry.resolveDynamic(storeId) ?: return null
        val file = ConfigPersistence.fileFor(appContext, descriptor)
        return when (mode) {
            "r" -> {
                if (!file.exists()) return null
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            }
            "w", "wt" -> {
                openAtomicWritePipe(appContext, storeId)
            }
            else -> null
        }
    }

    private fun openAtomicWritePipe(
        appContext: Context,
        storeId: String
    ): ParcelFileDescriptor? {
        val (readSide, writeSide) = ParcelFileDescriptor.createPipe()
        thread(name = "nexus-store-write-$storeId") {
            readSide.use { source ->
                runCatching {
                    val json = FileInputStream(source.fileDescriptor)
                        .bufferedReader(Charsets.UTF_8)
                        .use { it.readText() }
                    runBlocking {
                        XIpcStoreRepository.writeJson(appContext, storeId, json)
                    }
                }
            }
        }
        return writeSide
    }

    private fun isCallerAllowed(appContext: Context): Boolean {
        val callingUid = Binder.getCallingUid()
        if (callingUid == Process.myUid()) return true

        val callerPackages = appContext.packageManager.getPackagesForUid(callingUid) ?: emptyArray()
        val callingPkg = callingPackage
        return if (callingPkg != null) {
            callingPkg in XValues.appList && callingPkg in callerPackages
        } else {
            callerPackages.any { it in XValues.appList }
        }
    }

    override fun query(
        uri: Uri,
        p1: Array<out String>?,
        p2: String?,
        p3: Array<out String>?,
        p4: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0
}
