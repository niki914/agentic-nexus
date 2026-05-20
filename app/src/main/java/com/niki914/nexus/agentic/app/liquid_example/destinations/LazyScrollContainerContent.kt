package com.niki914.nexus.agentic.app.liquid_example.destinations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.niki914.nexus.agentic.app.liquid_example.BackdropDemoScaffold

@Composable
fun LazyScrollContainerContent() {
    BackdropDemoScaffold { backdrop ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16f.dp),
            verticalArrangement = Arrangement.spacedBy(16f.dp)
        ) {
            item {
                Spacer(Modifier.windowInsetsTopHeight(WindowInsets.systemBars))
            }
            items(100) {
                Box(
                    Modifier
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedCornerShape(32f.dp) },
                            effects = {
                                vibrancy()
                                lens(16f.dp.toPx(), 32f.dp.toPx())
                            }
                        )
                        .height(160f.dp)
                        .fillMaxWidth()
                )
            }
            item {
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
            }
        }
    }
}
