package com.niki914.nexus.agentic.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.niki914.nexus.agentic.app.ui.infra.HomeDemo
import com.niki914.nexus.agentic.mod.XService
import com.niki914.nexus.cb.BaseTheme
import com.niki914.nexus.h.util.RootUtils
import com.niki914.nexus.h.util.xlog
import com.niki914.nexus.ipc.HostApp
import com.niki914.nexus.ipc.XValues
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        xlog("MainActivity: POST_NOTIFICATIONS granted=$granted")
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestNotificationPermissionIfNeeded()

        setContent {
            BaseTheme {
                HomeDemo()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch(Dispatchers.IO) {
            val osFamily = RootUtils.getOsFamily()
            val preferredPkg = preferredHostAppFor(osFamily)?.packageName
            val candidatePkgs = buildList {
                if (preferredPkg != null) add(preferredPkg)
                addAll(XValues.appList.filter { it != preferredPkg })
            }
            val targetPkg = candidatePkgs.firstOrNull { pkg ->
                RootUtils.getPackageVersionCode(pkg) != null
            }
            if (targetPkg == null) {
                xlog("MainActivity: no supported host found, osFamily=$osFamily")
                return@launch
            }
            xlog("MainActivity: osFamily=$osFamily targetPkg=$targetPkg")
            val versionCode = RootUtils.getPackageVersionCode(targetPkg)
            if (versionCode != null) {
                xlog("currApp=$targetPkg versionCode=$versionCode")
                XService.refreshWebSettings(this@MainActivity, targetPkg, versionCode)
            } else {
                xlog("RootUtils: Failed to get version code for $targetPkg via su")
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }
        fun checkPermission() = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (checkPermission()) return
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun preferredHostAppFor(osFamily: RootUtils.OsFamily): HostApp? {
        return when (osFamily) {
            RootUtils.OsFamily.ColorOS -> HostApp.Breeno
            RootUtils.OsFamily.HyperOS -> HostApp.XiaoAi
            RootUtils.OsFamily.Unknown -> null
        }
    }
}