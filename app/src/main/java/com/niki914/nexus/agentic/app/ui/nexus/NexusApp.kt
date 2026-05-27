package com.niki914.nexus.agentic.app.ui.nexus

import android.app.Activity
import android.os.SystemClock
import android.widget.Toast

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
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
import com.niki914.nexus.agentic.app.ui.nexus.nav.NoTitle
import com.niki914.nexus.agentic.app.ui.nexus.nav.PageTitleSpec
import com.niki914.nexus.agentic.app.ui.nexus.nav.ResTitle
import com.niki914.nexus.agentic.app.ui.nexus.nav.SettingsHomePage
import com.niki914.nexus.agentic.app.ui.nexus.nav.TextTitle
import com.niki914.nexus.agentic.app.ui.nexus.nav.TopBarActionSpec

@Composable
fun NexusApp(
    startupAssistantUi: StartupAssistantUi,
    launchDecision: AppLaunchDecision,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val isDarkTheme = isSystemInDarkTheme()
    val actionIconTint = if (isDarkTheme) Color.White else Color.Black
    val rootBackToHomeWindowMillis = 2_000L
    val rootBackToHomeHint = stringResource(R.string.ui_root_back_to_home_hint)
    var homeMenuExpanded by remember { mutableStateOf(false) }
    var lastRootBackPressedAt by remember { mutableStateOf(0L) }
    fun openHomeMenu() {
        homeMenuExpanded = true
    }
    val initialPage = remember(launchDecision.initialPage) {
        attachPageActions(
            page = launchDecision.initialPage,
            onOpenHomeMenu = ::openHomeMenu,
        )
    }
    val controller = rememberNavigationController<NexusPage>(initialPage = initialPage)
    val navigator = controller.navigator
    val currentEntry = controller.currentEntry
    val currentPage = currentEntry.page
    val currentLeftAction = currentPage.leftAction
    val currentRightAction = currentPage.rightAction
    val showLeftButton = currentLeftAction != null && (currentLeftAction.onClick != null || controller.canGoBack)
    val showRightButton = currentRightAction != null
    val currentHomeChatViewModel = if (currentPage is HomePage) {
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
    val currentTitle = resolveTitle(currentPage.titleSpec)
    val clearMenuLabel = stringResource(R.string.ui_home_menu_clear)
    val settingsMenuLabel = stringResource(R.string.ui_settings_menu_entry)
    val screenState = rememberLiquidScreenState(
        title = currentTitle,
        showLeftButton = showLeftButton,
        showRightButton = showRightButton,
        showBlurLayer = currentPage.showBlurLayer,
    )

    fun closeHomeMenu() {
        homeMenuExpanded = false
    }

    fun push(page: NexusPage) {
        closeHomeMenu()
        navigator.push(attachPageActions(page = page, onOpenHomeMenu = ::openHomeMenu))
    }

    fun resetTo(page: NexusPage) {
        closeHomeMenu()
        navigator.resetTo(attachPageActions(page = page, onOpenHomeMenu = ::openHomeMenu))
    }

    BackHandler(enabled = true) {
        if (homeMenuExpanded) {
            closeHomeMenu()
        } else if (controller.canGoBack) {
            navigator.pop()
        } else {
            val now = SystemClock.elapsedRealtime()
            if (now - lastRootBackPressedAt <= rootBackToHomeWindowMillis) {
                activity?.moveTaskToBack(true)
            } else {
                lastRootBackPressedAt = now
                Toast.makeText(context.applicationContext, rootBackToHomeHint, Toast.LENGTH_SHORT).show() // TODO rm
            }
        }
    }

    LaunchedEffect(currentEntry.id, controller.lastDirection, currentTitle) {
        if (currentPage !is HomePage) {
            closeHomeMenu()
        }
        val onLeftClick = bindAction(currentLeftAction) { navigator.pop() }
        val onRightClick = bindAction(currentRightAction)

        when (controller.lastDirection) {
            TitleDirection.Forward -> screenState.navigateForward(
                title = currentTitle,
                showLeftButton = showLeftButton,
                showRightButton = showRightButton,
                showBlurLayer = currentPage.showBlurLayer,
                onLeftClick = onLeftClick,
                onRightClick = onRightClick,
            )

            TitleDirection.Back -> screenState.navigateBack(
                title = currentTitle,
                showLeftButton = showLeftButton,
                showRightButton = showRightButton,
                showBlurLayer = currentPage.showBlurLayer,
                onLeftClick = onLeftClick,
                onRightClick = onRightClick,
            )

            TitleDirection.None -> screenState.update(
                title = currentTitle,
                showLeftButton = showLeftButton,
                showRightButton = showRightButton,
                showBlurLayer = currentPage.showBlurLayer,
                onLeftClick = onLeftClick,
                onRightClick = onRightClick,
            )
        }
    }

    LiquidScreen(
        state = screenState,
        leftButton = currentLeftAction?.let { action ->
            {
                AnimatedContent(
                    targetState = action.icon,
                    transitionSpec = {
                        val iconAnimationSpec = tween<Float>(
                            durationMillis = 280,
                            easing = FastOutSlowInEasing,
                        )
                        (scaleIn(
                            initialScale = 1.18f,
                            animationSpec = iconAnimationSpec,
                        ) + fadeIn(animationSpec = iconAnimationSpec)).togetherWith(
                            scaleOut(
                                targetScale = 0.78f,
                                animationSpec = iconAnimationSpec,
                            ) + fadeOut(animationSpec = iconAnimationSpec)
                        )
                    },
                    label = "leftActionIcon",
                ) { imageVector ->
                    ActionBarVectorIcon(
                        imageVector = imageVector,
                        tint = actionIconTint,
                    )
                }
            }
        },
        rightButton = currentRightAction?.let { action ->
            {
                AnimatedContent(
                    targetState = action.icon,
                    transitionSpec = {
                        val iconAnimationSpec = tween<Float>(
                            durationMillis = 280,
                            easing = FastOutSlowInEasing,
                        )
                        (scaleIn(
                            initialScale = 1.18f,
                            animationSpec = iconAnimationSpec,
                        ) + fadeIn(animationSpec = iconAnimationSpec)).togetherWith(
                            scaleOut(
                                targetScale = 0.78f,
                                animationSpec = iconAnimationSpec,
                            ) + fadeOut(animationSpec = iconAnimationSpec)
                        )
                    },
                    label = "rightActionIcon",
                ) { imageVector ->
                    ActionBarVectorIcon(
                        imageVector = imageVector,
                        tint = actionIconTint,
                    )
                }
            }
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
                        onResetTo = ::resetTo,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = screenState.actionBarHeight.value, end = 12.dp),
            ) {
                DropdownMenu(
                    expanded = currentPage is HomePage && homeMenuExpanded,
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
private fun resolveTitle(titleSpec: PageTitleSpec): String {
    return when (titleSpec) {
        NoTitle -> ""
        is ResTitle -> stringResource(titleSpec.resId)
        is TextTitle -> titleSpec.value
    }
}

private fun bindAction(
    action: TopBarActionSpec?,
    fallback: (() -> Unit)? = null,
): (() -> Unit)? {
    return action?.onClick ?: fallback
}

private fun attachPageActions(
    page: NexusPage,
    onOpenHomeMenu: () -> Unit,
): NexusPage {
    return when (page) {
        is HomePage -> page.copy(onMenuClick = onOpenHomeMenu)
        else -> page
    }
}

@Composable
private fun ActionBarVectorIcon(
    imageVector: ImageVector,
    tint: Color,
    size: Dp = 20.dp,
) {
    Image(
        painter = rememberVectorPainter(imageVector),
        contentDescription = null,
        modifier = Modifier.size(size),
        colorFilter = ColorFilter.tint(tint),
    )
}
