package com.niki914.nexus.agentic.app.liquid_example.destinations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niki914.nexus.agentic.app.liquid_example.BackdropDemoScaffold
import com.niki914.nexus.agentic.app.liquid_example.components.LiquidButton

@Composable
fun ButtonsContent() {
    val textColor = if (isSystemInDarkTheme()) Color.White else Color.Black

    BackdropDemoScaffold { backdrop ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16f.dp)
        ) {
            LiquidButton(
                {},
                backdrop
            ) {
                BasicText(
                    "Transparent Liquid Button",
                    style = TextStyle(textColor, 15f.sp)
                )
            }
            LiquidButton(
                {},
                backdrop,
                surfaceColor = Color.White.copy(0.3f)
            ) {
                BasicText(
                    "Surface Liquid Button",
                    style = TextStyle(textColor, 15f.sp)
                )
            }
            LiquidButton(
                {},
                backdrop,
                tint = Color(0xFF0088FF)
            ) {
                BasicText(
                    "Tinted Liquid Button",
                    style = TextStyle(Color.White, 15f.sp)
                )
            }
            LiquidButton(
                {},
                backdrop,
                tint = Color(0xFFFF8D28)
            ) {
                BasicText(
                    "Tinted Liquid Button",
                    style = TextStyle(Color.White, 15f.sp)
                )
            }
        }
    }
}
