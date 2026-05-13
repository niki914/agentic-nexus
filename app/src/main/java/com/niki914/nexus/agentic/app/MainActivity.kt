package com.niki914.nexus.agentic.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.niki914.nexus.agentic.mod.XService
import com.niki914.nexus.cb.BaseTheme
import com.niki914.nexus.h.util.RootUtils
import com.niki914.nexus.h.util.xlog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.niki914.nexus.ipc.XValues

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

        // TODO Use liquid glass~

        setContent {
            BaseTheme { // 应用动态颜色主题
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {}
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch(Dispatchers.IO) {
            val osFamily = RootUtils.getOsFamily()
            val preferredPkg = when (osFamily) {
                RootUtils.OsFamily.ColorOS -> "com.heytap.speechassist" // TODO 硬编码不优雅，后续统一到一个新的枚举类，统一各个需要用到包名的地方
                RootUtils.OsFamily.HyperOS -> "com.miui.voiceassist"
                RootUtils.OsFamily.Unknown -> null
            }
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
        fun checkPermission() = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (checkPermission()) return
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}