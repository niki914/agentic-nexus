package com.niki914.nexus.ipc.cp

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import com.niki914.nexus.ipc.IpcContract

class SettingsContentProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        val appContext = context ?: return null
        
        val callingUid = android.os.Binder.getCallingUid()
        if (callingUid != android.os.Process.myUid()) {
            val callerPackages = appContext.packageManager.getPackagesForUid(callingUid) ?: emptyArray()
            val callingPkg = callingPackage
            if (callingPkg != null) {
                if (callingPkg !in com.niki914.nexus.ipc.XValues.appList || callingPkg !in callerPackages) {
                    return null
                }
            } else {
                if (callerPackages.none { it in com.niki914.nexus.ipc.XValues.appList }) {
                    return null
                }
            }
        }

        val resolvedMethod = IpcContract.Method.fromWire(method)
            ?: return super.call(method, arg, extras)
        return XProviderDispatcher.dispatch(
            context = appContext,
            method = resolvedMethod,
            extras = extras
        ) ?: super.call(method, arg, extras)
    }

    override fun query(uri: Uri, p1: Array<out String>?, p2: String?, p3: Array<out String>?, p4: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
