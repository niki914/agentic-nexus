package com.niki914.nexus.agentic.app.ui.infra.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.capsule.Continuity
import com.kyant.capsule.continuities.G2Continuity
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.LayoutDirection

/**
 * iOS 风分组卡片：外置章节标题 + 圆角卡片容器。
 * 排版抄自 ui/settings/StyledToggle 的 independent=true 分支，颜色走 MaterialTheme，
 * 深浅色由 colorScheme.surfaceContainer / onSurfaceVariant 自动适配。
 *
 * 卡片本身 clip 成圆角，方便子项（如 SettingsNavigationRow）在按压渐变时
 * 不会溢出圆角边界。
 */
@Composable
fun SettingsGroupCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val titleColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
    val cardColor = MaterialTheme.colorScheme.surfaceContainer
    val cardShape = G2RoundedCornerShape(28.dp)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = titleColor,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(cardColor, cardShape),
            content = content,
        )
    }
}

private data class G2RoundedCornerShape(
    val cornerRadius: Dp,
    val continuity: Continuity = G2Continuity(),
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val radius = with(density) { cornerRadius.toPx() }
            .coerceAtMost(size.minDimension / 2f)
        return continuity.createRoundedRectangleOutline(
            size = size,
            topLeft = radius,
            topRight = radius,
            bottomRight = radius,
            bottomLeft = radius,
        )
    }
}
