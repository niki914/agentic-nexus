package com.niki914.nexus.ipc

import android.content.Context
import android.os.Binder

internal object XCallerValidator {

    fun isAllowed(context: Context, callingPackage: String?): Boolean {
        val packageManager = context.packageManager
        val callingUid = Binder.getCallingUid()
        val packagesForUid = packageManager.getPackagesForUid(callingUid)?.toSet().orEmpty()
        val allowedPackages = XValues.allowedCallerPackages(context.packageName)

        if (packagesForUid.none { it in allowedPackages }) {
            return false
        }

        return callingPackage == null || (callingPackage in allowedPackages && callingPackage in packagesForUid)
    }
}