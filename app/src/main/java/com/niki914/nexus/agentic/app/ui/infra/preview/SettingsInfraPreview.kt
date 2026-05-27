package com.niki914.nexus.agentic.app.ui.infra.preview

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.niki914.nexus.agentic.app.ui.infra.component.SettingExpandableTextCard
import com.niki914.nexus.agentic.app.ui.infra.component.SettingNavigationItem
import com.niki914.nexus.agentic.app.ui.infra.component.SettingToggleItem
import com.niki914.nexus.agentic.app.ui.infra.component.SettingsGroupCard
import com.niki914.nexus.cb.BaseTheme

@Composable
private fun SettingsInfraPreviewContent() {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = "Settings Infra Preview",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )

        SettingsGroupCard(title = "基础组内 Item") {
            SettingNavigationItem(
                title = "模型提供商",
                summary = "管理当前可用的模型提供商、默认端点和模型选择入口。",
                currentState = "DeepSeek",
                onClick = {},
            )
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            SettingToggleItem(
                title = "启用工具调用",
                description = "允许模型在回答过程中调用已启用的本地工具与 MCP 工具。",
                checked = true,
                onCheckedChange = {},
            )
        }

        SettingsGroupCard(title = "长文案与 Disabled") {
            SettingNavigationItem(
                title = "这是一个很长很长的导航标题，用来确认标题在真实页面宽度下的观感是否还能保持稳定",
                summary = "这里是一段更长的摘要文案，用来验证 summary 在默认规则下会被限制为两行并正确省略，而不是把整个卡片撑到不可控的高度。",
                currentState = "已选择一个很长的当前状态值",
                enabled = false,
                onClick = {},
            )
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            SettingToggleItem(
                title = "同步远程配置",
                description = "这是一个很长的说明文案，用来验证 description 在禁用状态下仍然遵循两行截断，同时观察整体透明 item 语义没有被误改成独立卡片。",
                checked = false,
                enabled = false,
                onCheckedChange = {},
            )
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            SettingToggleItem(
                title = "仅标题开关项",
                description = null,
                checked = true,
                onCheckedChange = {},
            )
        }

        SettingExpandableTextCard(
            title = "折叠态可展开卡片",
            description = "折叠态需要展示单行预览，并保持与组容器接近的排版节奏。",
            value = "https://api.example.com/v1/chat/completions",
            onValueChange = {},
            placeholder = "请输入地址",
            preview = "这是一个非常长的折叠态预览文本，用来验证 preview 在默认规则下只保留一行并以省略号结尾，避免和正文段落风格混淆。",
            expanded = false,
        )

        SettingExpandableTextCard(
            title = "展开态可展开卡片",
            description = "展开后展示输入区域，说明文案仍然保持两行以内，避免和输入内容争抢层级。",
            value = "第一行内容\n第二行内容\n第三行内容",
            onValueChange = {},
            placeholder = "请输入多行内容",
            expanded = true,
        )

        SettingExpandableTextCard(
            title = "Disabled 可展开卡片",
            description = "用于确认 disabled 下的卡片底色、标题和辅助文案透明度都能稳定显示。",
            value = "已存在但不可编辑的内容",
            onValueChange = {},
            placeholder = "不可编辑",
            preview = "当前内容不可编辑",
            enabled = false,
            expanded = false,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(name = "Settings Infra Light", showBackground = true, widthDp = 420, heightDp = 1200)
@Composable
private fun SettingsInfraLightPreview() {
    BaseTheme(darkTheme = false, dynamicColor = false) {
        Surface {
            SettingsInfraPreviewContent()
        }
    }
}

@Preview(
    name = "Settings Infra Dark",
    showBackground = true,
    widthDp = 420,
    heightDp = 1200,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun SettingsInfraDarkPreview() {
    BaseTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            SettingsInfraPreviewContent()
        }
    }
}
