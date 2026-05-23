package com.niki914.nexus.agentic.app.ui.nexus

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.niki914.nexus.agentic.app.R
import com.niki914.nexus.agentic.app.ui.infra.LiquidScreen
import com.niki914.nexus.agentic.app.ui.infra.LiquidScreenSwipeContent
import com.niki914.nexus.agentic.app.ui.infra.TitleDirection
import com.niki914.nexus.agentic.app.ui.infra.nav.LocalNavigationEntry
import com.niki914.nexus.agentic.app.ui.infra.nav.rememberNavigationController
import com.niki914.nexus.agentic.app.ui.infra.rememberLiquidScreenState
import com.niki914.nexus.agentic.app.ui.nexus.model.HomeChatIntent
import com.niki914.nexus.agentic.app.ui.nexus.model.HomeChatViewModel
import com.niki914.nexus.agentic.app.ui.nexus.model.StartupAssistantUi
import com.niki914.nexus.agentic.app.ui.nexus.nav.HomePage
import com.niki914.nexus.agentic.app.ui.nexus.nav.NexusPage
import com.niki914.nexus.agentic.app.ui.nexus.nav.SettingsHomePage
import com.niki914.nexus.agentic.app.ui.nexus.nav.StartupPage

@Composable
fun NexusApp(
    startupAssistantUi: StartupAssistantUi,
) {
    val controller = rememberNavigationController<NexusPage>(initialPage = StartupPage)
    val navigator = controller.navigator
    val currentEntry = controller.currentEntry
    val currentPage = currentEntry.page
    val currentHomeChatViewModel = if (currentPage == HomePage) {
        remember(currentEntry) {
            val viewModelClass = HomeChatViewModel::class.java
            ViewModelProvider(
                currentEntry,
                ViewModelProvider.NewInstanceFactory(),
            )[viewModelClass.name, viewModelClass]
        }
    } else {
        null
    }
    val currentTitle = currentPage.titleRes?.let { stringResource(it) }.orEmpty()
    val clearMenuLabel = stringResource(R.string.nexus_home_menu_clear)
    val settingsMenuLabel = stringResource(R.string.nexus_settings_menu_entry)
    val screenState = rememberLiquidScreenState(
        title = currentTitle,
        showLeftButton = currentPage.showLeftButton,
        showRightButton = currentPage.showRightButton,
        showBlurLayer = currentPage.showBlurLayer,
    )
    var homeMenuExpanded by remember { mutableStateOf(false) }

    fun closeHomeMenu() {
        homeMenuExpanded = false
    }

    fun push(page: NexusPage) {
        closeHomeMenu()
        navigator.push(page)
    }

    BackHandler(enabled = homeMenuExpanded || controller.canGoBack) {
        if (homeMenuExpanded) {
            closeHomeMenu()
        } else {
            navigator.pop()
        }
    }

    LaunchedEffect(currentEntry.id, controller.lastDirection, currentTitle) {
        if (currentPage != HomePage) {
            closeHomeMenu()
        }
        val onLeftClick = if (currentPage.showLeftButton) {
            { navigator.pop(); Unit }
        } else {
            null
        }
        val onRightClick = if (currentPage == HomePage) {
            { homeMenuExpanded = true }
        } else {
            null
        }

        when (controller.lastDirection) {
            TitleDirection.Forward -> screenState.navigateForward(
                title = currentTitle,
                showLeftButton = currentPage.showLeftButton,
                showRightButton = currentPage.showRightButton,
                showBlurLayer = currentPage.showBlurLayer,
                onLeftClick = onLeftClick,
                onRightClick = onRightClick,
            )

            TitleDirection.Back -> screenState.navigateBack(
                title = currentTitle,
                showLeftButton = currentPage.showLeftButton,
                showRightButton = currentPage.showRightButton,
                showBlurLayer = currentPage.showBlurLayer,
                onLeftClick = onLeftClick,
                onRightClick = onRightClick,
            )

            TitleDirection.None -> screenState.update(
                title = currentTitle,
                showLeftButton = currentPage.showLeftButton,
                showRightButton = currentPage.showRightButton,
                showBlurLayer = currentPage.showBlurLayer,
                onLeftClick = onLeftClick,
                onRightClick = onRightClick,
            )
        }
    }

    LiquidScreen(
        state = screenState,
        rightButton = { Text(text = "⋯", fontSize = 20.sp) },
    ) { hazeState ->
        Box(modifier = Modifier.fillMaxSize()) {
            LiquidScreenSwipeContent(
                targetState = currentEntry,
                direction = controller.lastDirection,
                modifier = Modifier.fillMaxSize(),
            ) { entry ->
                CompositionLocalProvider(LocalNavigationEntry provides entry) {
                    NexusPageContent(
                        entry = entry,
                        topPadding = screenState.actionBarHeight.value,
                        hazeState = hazeState,
                        startupAssistantUi = startupAssistantUi,
                        onPush = ::push,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = screenState.actionBarHeight.value, end = 12.dp),
            ) {
                DropdownMenu(
                    expanded = currentPage == HomePage && homeMenuExpanded,
                    onDismissRequest = ::closeHomeMenu,
                ) {
                    DropdownMenuItem(
                        text = { Text(clearMenuLabel) },
                        onClick = {
                            closeHomeMenu()
                            currentHomeChatViewModel?.sendIntent(HomeChatIntent.ClearConversation)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(settingsMenuLabel) },
                        onClick = { push(SettingsHomePage) },
                    )
                }
            }
        }
    }
}
