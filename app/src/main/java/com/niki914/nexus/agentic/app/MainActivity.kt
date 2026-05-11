package com.niki914.nexus.agentic.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.niki914.nexus.agentic.mod.refreshWebSettings
import com.niki914.nexus.cb.BaseTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            requestPermission(Manifest.permission.POST_NOTIFICATIONS)
//        }

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
            val targetPkg = "com.heytap.speechassist"
            val versionCode = com.niki914.nexus.h.util.RootUtils.getPackageVersionCode(targetPkg)
            if (versionCode != null) {
                refreshWebSettings(targetPkg, versionCode)
            } else {
                com.niki914.nexus.h.util.xlog("RootUtils: Failed to get version code for $targetPkg via su")
            }
        }
    }
}