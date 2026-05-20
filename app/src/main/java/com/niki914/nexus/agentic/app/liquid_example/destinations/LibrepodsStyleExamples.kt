package com.niki914.nexus.agentic.app.liquid_example.destinations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.niki914.nexus.agentic.app.liquid_example.BackdropDemoScaffold
import com.niki914.nexus.agentic.app.liquid_example.components.ConfirmationDialog
import com.niki914.nexus.agentic.app.liquid_example.components.StyledIconButton

@Composable
fun LibrepodsStyleExamples() {
    val contentColor = if (isSystemInDarkTheme()) Color.White else Color.Black

    BackdropDemoScaffold { backdrop ->
        val showDialog = remember { mutableStateOf(false) }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            StyledIconButton(
                onClick = { showDialog.value = true },
                backdrop = backdrop,
                icon = "⚙",
                iconTint = contentColor
            )

            Text(
                text = "Show Confirmation Dialog",
                color = contentColor,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        ConfirmationDialog(
            showDialog = showDialog,
            title = "Bypass compatibility check",
            message = "This action may cause unexpected behavior.",
            confirmText = "Bypass",
            dismissText = "Cancel",
            onConfirm = { showDialog.value = false },
            onDismiss = { showDialog.value = false },
            backdrop = backdrop
        )
    }
}
