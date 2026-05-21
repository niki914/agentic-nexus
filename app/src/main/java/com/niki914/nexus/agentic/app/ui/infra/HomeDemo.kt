package com.niki914.nexus.agentic.app.ui.infra

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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niki914.nexus.agentic.app.ui.infra.demo.HomePageContent
import com.niki914.nexus.agentic.app.ui.infra.demo.MorePageContent
import com.niki914.nexus.agentic.app.ui.infra.demo.SettingsGroupPageContent
import com.niki914.nexus.agentic.app.ui.infra.demo.SubSettingPageContent
import com.niki914.nexus.agentic.app.ui.infra.nav.DemoPage
import com.niki914.nexus.agentic.app.ui.infra.nav.HomePage
import com.niki914.nexus.agentic.app.ui.infra.nav.LocalNavigationEntry
import com.niki914.nexus.agentic.app.ui.infra.nav.MorePage
import com.niki914.nexus.agentic.app.ui.infra.nav.NavigationEntry
import com.niki914.nexus.agentic.app.ui.infra.nav.SettingsGroupPage
import com.niki914.nexus.agentic.app.ui.infra.nav.SubSettingPage
import com.niki914.nexus.agentic.app.ui.infra.nav.rememberNavigationController
import dev.chrisbanes.haze.HazeState

@Composable
fun HomeDemo() {
    val controller = rememberNavigationController<DemoPage>(initialPage = HomePage)
    val navigator = controller.navigator
    val currentEntry = controller.currentEntry
    val currentPage = currentEntry.page
    val screenState = rememberLiquidScreenState(
        title = currentPage.title,
        showLeftButton = currentPage.showLeftButton,
        showRightButton = currentPage.showRightButton,
    )
    var homeMenuExpanded by remember { mutableStateOf(false) }

    fun closeHomeMenu() {
        homeMenuExpanded = false
    }

    fun push(page: DemoPage) {
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

    LaunchedEffect(currentEntry.id, controller.lastDirection) {
        if (currentPage != HomePage) {
            homeMenuExpanded = false
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
                title = currentPage.title,
                showLeftButton = currentPage.showLeftButton,
                showRightButton = currentPage.showRightButton,
                onLeftClick = onLeftClick,
                onRightClick = onRightClick,
            )

            TitleDirection.Back -> screenState.navigateBack(
                title = currentPage.title,
                showLeftButton = currentPage.showLeftButton,
                showRightButton = currentPage.showRightButton,
                onLeftClick = onLeftClick,
                onRightClick = onRightClick,
            )

            TitleDirection.None -> screenState.update(
                title = currentPage.title,
                showLeftButton = currentPage.showLeftButton,
                showRightButton = currentPage.showRightButton,
                onLeftClick = onLeftClick,
                onRightClick = onRightClick,
            )
        }
    }

    LiquidScreen(
        state = screenState,
        rightButton = { Text("☰", fontSize = 20.sp) },
    ) { hazeState ->
        Box(modifier = Modifier.fillMaxSize()) {
            LiquidScreenSwipeContent(
                targetState = currentEntry,
                direction = controller.lastDirection,
                modifier = Modifier.fillMaxSize(),
            ) { entry ->
                CompositionLocalProvider(LocalNavigationEntry provides entry) {
                    DemoPageContent(
                        entry = entry,
                        topPadding = screenState.actionBarHeight.value,
                        hazeState = hazeState,
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
                        text = { Text("进入 MorePage") },
                        onClick = { push(MorePage) },
                    )
                    DropdownMenuItem(
                        text = { Text("进入 SettingsGroup1") },
                        onClick = { push(SettingsGroupPage("group1")) },
                    )
                    DropdownMenuItem(
                        text = { Text("进入 SubSetting00") },
                        onClick = { push(SubSettingPage("subsetting00")) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DemoPageContent(
    entry: NavigationEntry<DemoPage>,
    topPadding: Dp,
    hazeState: HazeState,
    onPush: (DemoPage) -> Unit,
) {
    when (val page = entry.page) {
        HomePage -> HomePageContent(
            topPadding = topPadding,
            hazeState = hazeState,
            onOpenSettingsGroup1 = { onPush(SettingsGroupPage("group1")) },
            onOpenSubSetting00 = { onPush(SubSettingPage("subsetting00")) },
        )

        MorePage -> MorePageContent(
            topPadding = topPadding,
            hazeState = hazeState,
            onOpenGroup = { groupId -> onPush(SettingsGroupPage(groupId)) },
        )

        is SettingsGroupPage -> SettingsGroupPageContent(
            groupId = page.groupId,
            topPadding = topPadding,
            hazeState = hazeState,
            onOpenSubSetting00 = { onPush(SubSettingPage("subsetting00")) },
        )

        is SubSettingPage -> SubSettingPageContent(
            settingId = page.settingId,
            topPadding = topPadding,
            hazeState = hazeState,
        )
    }
}
