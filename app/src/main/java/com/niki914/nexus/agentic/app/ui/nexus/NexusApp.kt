package com.niki914.nexus.agentic.app.ui.nexus

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreHoriz
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
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
import com.niki914.nexus.agentic.app.ui.nexus.model.AppLaunchDecision
import com.niki914.nexus.agentic.app.ui.nexus.model.StartupAssistantUi
import com.niki914.nexus.agentic.app.ui.nexus.nav.HomePage
import com.niki914.nexus.agentic.app.ui.nexus.nav.NexusPage
import com.niki914.nexus.agentic.app.ui.nexus.nav.SettingsHomePage

@Composable
fun NexusApp(
    startupAssistantUi: StartupAssistantUi,
    launchDecision: AppLaunchDecision,
) {
    val isDarkTheme = isSystemInDarkTheme()
    val actionIconTint = if (isDarkTheme) Color.White else Color.Black
    val controller = rememberNavigationController<NexusPage>(initialPage = launchDecision.initialPage)
    val navigator = controller.navigator
    val currentEntry = controller.currentEntry
    val currentPage = currentEntry.page
    val canNavigateBack = currentPage.showLeftButton && controller.canGoBack
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
        showLeftButton = canNavigateBack,
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
        val onLeftClick = if (canNavigateBack) {
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
                showLeftButton = canNavigateBack,
                showRightButton = currentPage.showRightButton,
                showBlurLayer = currentPage.showBlurLayer,
                onLeftClick = onLeftClick,
                onRightClick = onRightClick,
            )

            TitleDirection.Back -> screenState.navigateBack(
                title = currentTitle,
                showLeftButton = canNavigateBack,
                showRightButton = currentPage.showRightButton,
                showBlurLayer = currentPage.showBlurLayer,
                onLeftClick = onLeftClick,
                onRightClick = onRightClick,
            )

            TitleDirection.None -> screenState.update(
                title = currentTitle,
                showLeftButton = canNavigateBack,
                showRightButton = currentPage.showRightButton,
                showBlurLayer = currentPage.showBlurLayer,
                onLeftClick = onLeftClick,
                onRightClick = onRightClick,
            )
        }
    }

    LiquidScreen(
        state = screenState,
        leftButton = {
            ActionBarImage(
                painter = rememberVectorPainter(Icons.AutoMirrored.Filled.ArrowBack),
                tint = actionIconTint,
            )
        },
        rightButton = {
            ActionBarImage(
                painter = rememberVectorPainter(Icons.Default.MoreHoriz),
                tint = actionIconTint,
            )
        },
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
                        onResetTo = navigator::resetTo,
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

@Composable
private fun ActionBarImage(
    painter: androidx.compose.ui.graphics.painter.Painter,
    tint: Color,
    size: Dp = 20.dp,
) {
    Image(
        painter = painter,
        contentDescription = null,
        modifier = Modifier.size(size),
        colorFilter = ColorFilter.tint(tint),
    )
}
