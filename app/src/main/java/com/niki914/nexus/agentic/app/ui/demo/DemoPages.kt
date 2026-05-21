package com.niki914.nexus.agentic.app.ui.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.niki914.nexus.agentic.app.ui.demo.nav.HomePage
import com.niki914.nexus.agentic.app.ui.demo.nav.MorePage
import com.niki914.nexus.agentic.app.ui.demo.nav.SettingsGroupPage
import com.niki914.nexus.agentic.app.ui.demo.nav.SubSettingPage
import com.niki914.nexus.agentic.app.ui.infra.nav.pageViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@Composable
fun HomePageContent(
    topPadding: Dp,
    hazeState: HazeState,
    onOpenSettingsGroup1: () -> Unit,
    onOpenSubSetting00: () -> Unit,
) {
    val viewModel = pageViewModel<HomePageViewModel>()
    val uiState by viewModel.uiStateFlow.collectAsState()

    DemoPageContainer(
        title = HomePage.title,
        routeKey = HomePage.routeKey,
        secondsAlive = uiState.secondsAlive,
        topPadding = topPadding,
        hazeState = hazeState,
    ) {
        Text(
            text = "右上角 more(menu) 是主入口，这里只保留两个直达入口做状态验证。",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FilledTonalButton(onClick = onOpenSettingsGroup1) {
            Text("直接进入 SettingsGroup1")
        }
        FilledTonalButton(onClick = onOpenSubSetting00) {
            Text("直接进入 SubSetting00")
        }
    }
}

@Composable
fun MorePageContent(
    topPadding: Dp,
    hazeState: HazeState,
    onOpenGroup: (String) -> Unit,
) {
    val viewModel = pageViewModel<MorePageViewModel>()
    val uiState by viewModel.uiStateFlow.collectAsState()

    DemoPageContainer(
        title = MorePage.title,
        routeKey = MorePage.routeKey,
        secondsAlive = uiState.secondsAlive,
        topPadding = topPadding,
        hazeState = hazeState,
    ) {
        Text(
            text = "这里模拟 MorePage，下钻到三个分组页。",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FilledTonalButton(onClick = { onOpenGroup("group1") }) {
            Text("进入 SettingsGroup1")
        }
        FilledTonalButton(onClick = { onOpenGroup("group2") }) {
            Text("进入 SettingsGroup2")
        }
        FilledTonalButton(onClick = { onOpenGroup("group3") }) {
            Text("进入 SettingsGroup3")
        }
    }
}

@Composable
fun SettingsGroupPageContent(
    groupId: String,
    topPadding: Dp,
    hazeState: HazeState,
    onOpenSubSetting00: () -> Unit,
) {
    val viewModel = pageViewModel<SettingsGroupViewModel>(key = groupId)
    val uiState by viewModel.uiStateFlow.collectAsState()
    val page = SettingsGroupPage(groupId = groupId)

    DemoPageContainer(
        title = page.title,
        routeKey = page.routeKey,
        secondsAlive = uiState.secondsAlive,
        topPadding = topPadding,
        hazeState = hazeState,
    ) {
        Text(
            text = "当前是分组页面 $groupId，可继续进入唯一子设置页。",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FilledTonalButton(onClick = onOpenSubSetting00) {
            Text("进入 SubSetting00")
        }
    }
}

@Composable
fun SubSettingPageContent(
    settingId: String,
    topPadding: Dp,
    hazeState: HazeState,
) {
    val viewModel = pageViewModel<SubSettingViewModel>(key = settingId)
    val uiState by viewModel.uiStateFlow.collectAsState()
    val page = SubSettingPage(settingId = settingId)

    DemoPageContainer(
        title = page.title,
        routeKey = page.routeKey,
        secondsAlive = uiState.secondsAlive,
        topPadding = topPadding,
        hazeState = hazeState,
    ) {
        Text(
            text = "这里只放占位文本，用来验证最深层页面的 VM 生命周期。",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DemoPageContainer(
    title: String,
    routeKey: String,
    secondsAlive: Int,
    topPadding: Dp,
    hazeState: HazeState,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .hazeSource(hazeState)
            .verticalScroll(rememberScrollState())
            .padding(top = topPadding)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "routeKey: $routeKey",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "页面已存在秒数: ${secondsAlive}s",
            style = MaterialTheme.typography.titleMedium,
        )
        content()
    }
}
