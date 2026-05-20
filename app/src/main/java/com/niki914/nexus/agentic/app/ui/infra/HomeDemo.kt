package com.niki914.nexus.agentic.app.ui.infra

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource


// --- LiquidScreen 用例: 首页 ↔ 详情 双页导航 ---

@Composable
fun HomeDemo() {
    val state = rememberLiquidScreenState(
        title = "首页",
        showLeftButton = false,
        showRightButton = true,
        onRightClick = null,
    )

    var page by remember { mutableIntStateOf(0) }

    LaunchedEffect(page) {
        if (page == 0) {
            state.navigateBack( // TODO 状态机
                title = "首页",
                showLeftButton = false,
                showRightButton = true,
                onLeftClick = null,
                onRightClick = { page = 1 },
            )
        } else {
            state.navigateForward(
                title = "详情",
                showLeftButton = true,
                showRightButton = false,
                onLeftClick = { page = 0 },
                onRightClick = null,
            )
        }
    }

    val items = List(30) { i -> "项目 ${i + 1}" }

    LiquidScreen(
        state = state,
        rightButton = {
            Text("☰", fontSize = 20.sp)
        },
    ) { hazeState ->
        LiquidScreenSwipeContent(
            targetState = page,
            modifier = Modifier
                .fillMaxSize(),
            direction = state.navigationDirection,
        ) { currentPage ->
            when (currentPage) {
                0 -> HomeListPage(
                    items = items,
                    topPadding = state.actionBarHeight.value,
                    hazeState = hazeState,
                    onItemClick = { page = 1 },
                )

                else -> DetailPage(
                    topPadding = state.actionBarHeight.value,
                    hazeState = hazeState,
                )
            }
        }
    }
}

@Composable
private fun HomeListPage(
    items: List<String>,
    topPadding: Dp,
    hazeState: HazeState,
    onItemClick: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .hazeSource(hazeState),
        contentPadding = PaddingValues(top = topPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { Spacer(Modifier.height(4.dp)) }
        items(items) { label ->
            ListItemRow(
                label = label,
                onClick = onItemClick,
            )
        }
    }
}

@Composable
private fun DetailPage(
    topPadding: Dp,
    hazeState: HazeState,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .hazeSource(hazeState)
            .padding(top = topPadding)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "详情页",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "现在内容区域也会跟随前进/后退方向做左右切换，而不是只有标题在动。",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        repeat(4) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(20.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "内容卡片 ${index + 1}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "这里可以放详情信息、设置分组，或者下一层操作入口。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ListItemRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label.first().toString(),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "这是 $label 的描述信息",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
