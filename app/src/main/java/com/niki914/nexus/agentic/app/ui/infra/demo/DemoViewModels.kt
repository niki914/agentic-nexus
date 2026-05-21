package com.niki914.nexus.agentic.app.ui.infra.demo

import androidx.lifecycle.viewModelScope
import com.niki914.nexus.cb.ComposeMVIViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface DemoPageIntent {
    data object StartTicker : DemoPageIntent
}

data class DemoPageState(
    val secondsAlive: Int = 0,
)

sealed interface DemoPageEffect

abstract class SecondsAliveViewModel :
    ComposeMVIViewModel<DemoPageIntent, DemoPageState, DemoPageEffect>() {

    private var tickerJob: Job? = null

    init {
        sendIntent(DemoPageIntent.StartTicker)
    }

    override fun initUiState(): DemoPageState {
        return DemoPageState()
    }

    override suspend fun handleIntent(intent: DemoPageIntent) {
        when (intent) {
            DemoPageIntent.StartTicker -> startTickerIfNeeded()
        }
    }

    private fun startTickerIfNeeded() {
        if (tickerJob != null) return
        tickerJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                updateState {
                    copy(secondsAlive = secondsAlive + 1)
                }
            }
        }
    }
}

class HomePageViewModel : SecondsAliveViewModel()

class MorePageViewModel : SecondsAliveViewModel()

class SettingsGroupViewModel : SecondsAliveViewModel()

class SubSettingViewModel : SecondsAliveViewModel()
