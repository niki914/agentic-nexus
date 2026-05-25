package com.niki914.nexus.agentic.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.niki914.nexus.agentic.app.ui.nexus.NexusApp
import com.niki914.nexus.agentic.app.ui.nexus.model.AppLaunchDecision
import com.niki914.nexus.agentic.mod.XService
import com.niki914.nexus.cb.BaseTheme
import com.niki914.nexus.h.util.ContextProvider
import com.niki914.nexus.h.util.OsUtils
import com.niki914.nexus.h.util.xlog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

// tag:niki914 | tag:nexus-x-log | message:niki914 | message:nexus-x-log
class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        xlog("MainActivity: POST_NOTIFICATIONS granted=$granted")
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ContextProvider.provide(applicationContext)

        requestNotificationPermissionIfNeeded(notificationPermissionLauncher)
        val startupAssistantUi = resolveStartupAssistantUi()
        val launchDecision = runBlocking {
            AppLaunchDecision.resolve(
                settings = XService.getLocalSettings(this@MainActivity),
                startupAssistantUi = startupAssistantUi,
            )
        }

        setContent {
            BaseTheme {
                NexusApp(
                    startupAssistantUi = startupAssistantUi,
                    launchDecision = launchDecision,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch(Dispatchers.IO) {
            val osFamily = OsUtils.getCurr()
            val targetPkg = resolveTargetHostPackage(osFamily)
            if (targetPkg == null) {
                xlog("MainActivity: no supported host found, osFamily=$osFamily")
                return@launch
            }
            xlog("MainActivity: osFamily=$osFamily targetPkg=$targetPkg")
            val versionCode = getInstalledPackageVersionCode(targetPkg)
            if (versionCode != null) {
                xlog("currApp=$targetPkg versionCode=$versionCode")
                XService.refreshWebSettings(this@MainActivity, targetPkg, versionCode)
            } else {
                xlog("MainActivity: failed to get version code for $targetPkg via PackageManager")
            }
        }
    }
}
