package com.niki914.nexus.agentic.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import com.niki914.nexus.agentic.repo.XRepo
import com.niki914.nexus.agentic.runtime.createAppRuntimeBridge
import com.niki914.nexus.agentic.runtime.settings.RuntimeEnvironment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class App : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        XRepo.init(this.applicationContext)
        RuntimeEnvironment.install(createAppRuntimeBridge())
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        DynamicColors.applyToActivitiesIfAvailable(this)
        applicationScope.launch {
            XRepo.tryPutDefaultSettings()
        }
    }
}
