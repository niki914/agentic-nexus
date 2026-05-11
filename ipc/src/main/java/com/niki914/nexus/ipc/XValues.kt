package com.niki914.nexus.ipc

object XValues {
    val appList: List<String> = listOf(
        "com.heytap.speechassist"
    )

    internal fun allowedCallerPackages(hostPackage: String): Set<String> {
        return buildSet {
            add(hostPackage)
            addAll(appList)
        }
    }
}