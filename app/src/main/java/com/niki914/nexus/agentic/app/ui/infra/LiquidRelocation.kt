package com.niki914.nexus.agentic.app.ui.infra

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import kotlin.math.roundToInt

@Immutable
internal data class LiquidRelocationTargetSnapshot(
    val id: String,
    val boundsInRoot: Rect,
    val isFocused: Boolean,
    val preferredTopMarginPx: Int = 0,
    val preferredBottomMarginPx: Int = 0,
)

@Stable
internal class LiquidRelocationState(
    initialImeBottomPx: Int = 0,
    initialTopObstaclePx: Int = 0,
    initialContentRootHeightPx: Int = 0,
    initialActiveTarget: LiquidRelocationTargetSnapshot? = null,
) {
    var imeBottomPx by mutableIntStateOf(initialImeBottomPx)
    var topObstaclePx by mutableIntStateOf(initialTopObstaclePx)
    var contentRootHeightPx by mutableIntStateOf(initialContentRootHeightPx)
    var activeTarget: LiquidRelocationTargetSnapshot? by mutableStateOf(
        initialActiveTarget?.takeIf { it.isFocused }
    )

    fun updateActiveTarget(snapshot: LiquidRelocationTargetSnapshot?) {
        activeTarget = snapshot?.takeIf { it.isFocused }
    }

    fun clearTarget(id: String) {
        if (activeTarget?.id == id) {
            activeTarget = null
        }
    }

    fun calculateTranslationYPx(): Int {
        val target = activeTarget ?: return 0
        val safeTop = topObstaclePx + target.preferredTopMarginPx
        val safeBottom = contentRootHeightPx - imeBottomPx - target.preferredBottomMarginPx
        val targetTop = target.boundsInRoot.top.roundToInt()
        val targetBottom = target.boundsInRoot.bottom.roundToInt()

        return when {
            targetBottom > safeBottom -> safeBottom - targetBottom
            targetTop < safeTop -> safeTop - targetTop
            else -> 0
        }
    }
}

internal val LocalLiquidRelocationState = compositionLocalOf<LiquidRelocationState?> { null }
