package com.niki914.nexus.agentic.app

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import com.niki914.nexus.agentic.app.ui.nexus.model.StartupAssistantUi
import com.niki914.nexus.h.util.OsFamily
import com.niki914.nexus.h.util.OsUtils
import com.niki914.nexus.ipc.HostApp
import com.niki914.nexus.ipc.XValues

fun Activity.getInstalledPackageVersionCode(packageName: String): Long? {
    return try {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
    } catch (_: PackageManager.NameNotFoundException) {
        // TODO P1 report
        null
    }
}

fun Activity.requestNotificationPermissionIfNeeded(launcher: ActivityResultLauncher<String>) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return
    }
    fun checkPermission() = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
    if (checkPermission()) return
    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
}

fun preferredHostAppFor(osFamily: OsFamily): HostApp? {
    return when (osFamily) {
        OsFamily.ColorOS -> HostApp.Breeno
        OsFamily.HyperOS -> HostApp.XiaoAi
        OsFamily.Unknown -> null
    }
}

fun Activity.resolveTargetHostPackage(osFamily: OsFamily): String? {
    val preferredPkg = preferredHostAppFor(osFamily)?.packageName
    val candidatePkgs = buildList {
        if (preferredPkg != null) add(preferredPkg)
        addAll(XValues.appList.filter { it != preferredPkg })
    }
    return candidatePkgs.firstOrNull { pkg ->
        getInstalledPackageVersionCode(pkg) != null
    }
}

fun Activity.resolveStartupAssistantUi(): StartupAssistantUi {
    return StartupAssistantUi.fromOsFamily(OsUtils.getCurr())
}
