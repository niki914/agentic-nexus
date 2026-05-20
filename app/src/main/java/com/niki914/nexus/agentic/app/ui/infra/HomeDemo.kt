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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


// --- LiquidScreen 用例: 首页 ↔ 详情 双页导航 ---

@Composable
fun HomeDemo() {
    val state = rememberLiquidScreenState(
        title = "首页",
        showLeftButton = false,
        showRightButton = true,
        onRightClick = { /* no-op, handled per-page below */ },
    )

    var page by remember { mutableIntStateOf(0) }

    // Keep state click handlers in sync with current page
    state.onRightClick = if (page == 0) {
        { page = 1 }
    } else {
        null
    }
    state.onLeftClick = if (page == 1) {
        { page = 0 }
    } else {
        null
    }

    // React to page changes → update navigation bar
    if (page == 0) {
        state.update(
            title = "首页",
            showLeftButton = false,
            showRightButton = true,
        )
    } else {
        state.update(
            title = "详情",
            showLeftButton = true,
            showRightButton = false,
        )
    }

    val items = List(30) { i -> "项目 ${i + 1}" }

    LiquidScreen(
        state = state,
        rightButton = {
            Text("☰", fontSize = 20.sp)
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = state.actionBarHeight.value),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { Spacer(Modifier.height(4.dp)) }
            items(items.size) { i ->
                ListItemRow(
                    label = items[i],
                    onClick = {
                        if (page == 0) page = 1
                    },
                )
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