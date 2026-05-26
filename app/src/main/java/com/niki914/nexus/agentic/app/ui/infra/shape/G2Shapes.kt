package com.niki914.nexus.agentic.app.ui.infra.shape

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.kyant.capsule.Continuity
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.continuities.G2Continuity

fun G2FieldShape(cornerRadius: Dp = 28.dp): Shape =
    G2RoundedCornerShape(cornerRadius)

fun G2CardShape(cornerRadius: Dp = 28.dp): Shape =
    G2RoundedCornerShape(cornerRadius)

fun G2BubbleShape(cornerRadius: Dp = 24.dp): Shape =
    G2RoundedCornerShape(cornerRadius)

fun G2CapsuleShape(): Shape =
    ContinuousCapsule(G2Continuity())

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
