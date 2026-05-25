package com.niki914.nexus.agentic.app.ui.nexus.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.annotation.DrawableRes
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import com.niki914.nexus.agentic.app.ui.infra.component.TintLiquidButton
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

data class SelectionOption(
    val id: String,
    val title: String,
    @DrawableRes val leadingIconRes: Int,
    val tintLeadingIcon: Boolean = true,
    val darkContainerColor: Color = Color.Unspecified,
    val lightContainerColor: Color = Color.Unspecified,
    val darkContentColor: Color = Color.Unspecified,
    val lightContentColor: Color = Color.Unspecified,
    val onClick: () -> Unit,
)

@Composable
fun SelectionPageContent(
    topPadding: Dp,
    hazeState: HazeState,
    options: List<SelectionOption>,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .hazeSource(hazeState)
            .verticalScroll(rememberScrollState())
            .padding(top = topPadding)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        options.forEach { option ->
            TintLiquidButton(
                text = option.title,
                leadingIconRes = option.leadingIconRes,
                trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                tintLeadingIcon = option.tintLeadingIcon,
                darkContainerColor = option.darkContainerColor,
                lightContainerColor = option.lightContainerColor,
                darkContentColor = option.darkContentColor,
                lightContentColor = option.lightContentColor,
                onClick = option.onClick,
            )
        }
    }
}
