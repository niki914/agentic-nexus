package com.niki914.nexus.agentic.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import dev.chrisbanes.haze.hazeSource

@Composable
fun SettingsScreen(navController: NavController = rememberNavController()) {
    val scrollState = rememberScrollState()

    StyledScaffold(title = "Settings") { topPadding, hazeState, bottomPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(topPadding))

            val isDarkTheme = isSystemInDarkTheme()
            val backgroundColor = if (isDarkTheme) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
            val textColor = if (isDarkTheme) Color.White else Color.Black

            // ---- state ----
            var showPhoneBatteryInWidget by remember { mutableStateOf(false) }
            var showBottomSheetPopup by remember { mutableStateOf(true) }
            var showIslandPopup by remember { mutableStateOf(false) }
            var conversationalAwarenessPauseMusic by remember { mutableStateOf(true) }
            var relativeConversationalAwarenessVolume by remember { mutableStateOf(false) }
            var disconnectWhenNotWearing by remember { mutableStateOf(false) }
            var takeoverDisconnected by remember { mutableStateOf(true) }
            var takeoverIdle by remember { mutableStateOf(false) }
            var takeoverMusic by remember { mutableStateOf(true) }
            var takeoverCall by remember { mutableStateOf(false) }
            var takeoverRingingCall by remember { mutableStateOf(true) }
            var takeoverMediaStart by remember { mutableStateOf(false) }
            var useAlternateHeadTracking by remember { mutableStateOf(false) }

            // ---- Widget ----
            StyledToggle(
                title = "Widget",
                label = "Show Phone Battery in Widget",
                description = "Displays the phone battery level alongside your AirPods in the battery widget.",
                checked = showPhoneBatteryInWidget,
                onCheckedChange = { showPhoneBatteryInWidget = it }
            )

            // ---- Popup Animations ----
            Text(
                text = "Popup Animations",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor.copy(alpha = 0.6f),
                ),
                modifier = Modifier.padding(16.dp, bottom = 2.dp, top = 24.dp)
            )

            Spacer(modifier = Modifier.height(2.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundColor, RoundedCornerShape(28.dp))
                    .padding(vertical = 4.dp)
            ) {
                StyledToggle(
                    label = "Show Bottom Sheet Popup",
                    description = "Show a bottom sheet popup when connecting or disconnecting your AirPods.",
                    checked = showBottomSheetPopup,
                    onCheckedChange = { showBottomSheetPopup = it },
                    independent = false
                )

                HorizontalDivider(
                    thickness = 1.dp,
                    color = Color(0x40888888),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                StyledToggle(
                    label = "Show Island Popup",
                    description = "Show a Dynamic Island style popup when connecting or disconnecting your AirPods.",
                    checked = showIslandPopup,
                    onCheckedChange = { showIslandPopup = it },
                    independent = false
                )
            }

            // ---- Conversational Awareness ----
            Text(
                text = "Conversational Awareness",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor.copy(alpha = 0.6f),
                ),
                modifier = Modifier.padding(16.dp, bottom = 2.dp, top = 24.dp)
            )

            Spacer(modifier = Modifier.height(2.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundColor, RoundedCornerShape(28.dp))
                    .padding(vertical = 4.dp)
            ) {
                StyledToggle(
                    label = "Pause Music",
                    description = "Automatically pause music when you start speaking.",
                    checked = conversationalAwarenessPauseMusic,
                    onCheckedChange = { conversationalAwarenessPauseMusic = it },
                    independent = false
                )

                HorizontalDivider(
                    thickness = 1.dp,
                    color = Color(0x40888888),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                StyledToggle(
                    label = "Relative Volume",
                    description = "Adjust conversational awareness volume relative to the current media volume.",
                    checked = relativeConversationalAwarenessVolume,
                    onCheckedChange = { relativeConversationalAwarenessVolume = it },
                    independent = false
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---- Ear Detection ----
            StyledToggle(
                title = "Ear Detection",
                label = "Disconnect When Not Wearing",
                description = "Automatically disconnect your AirPods when you take them out of your ears.",
                checked = disconnectWhenNotWearing,
                onCheckedChange = { disconnectWhenNotWearing = it }
            )

            // ---- Takeover AirPods State ----
            Text(
                text = "Takeover AirPods State",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor.copy(alpha = 0.6f),
                ),
                modifier = Modifier.padding(16.dp, bottom = 2.dp, top = 24.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundColor, RoundedCornerShape(28.dp))
                    .padding(vertical = 4.dp)
            ) {
                StyledToggle(
                    label = "Takeover Disconnected",
                    description = "Take over when AirPods are disconnected from the device.",
                    checked = takeoverDisconnected,
                    onCheckedChange = { takeoverDisconnected = it },
                    independent = false
                )
                HorizontalDivider(
                    thickness = 1.dp,
                    color = Color(0x40888888),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                StyledToggle(
                    label = "Takeover Idle",
                    description = "Take over when AirPods are idle and not playing audio.",
                    checked = takeoverIdle,
                    onCheckedChange = { takeoverIdle = it },
                    independent = false
                )
                HorizontalDivider(
                    thickness = 1.dp,
                    color = Color(0x40888888),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                StyledToggle(
                    label = "Takeover Music",
                    description = "Take over when music is actively playing.",
                    checked = takeoverMusic,
                    onCheckedChange = { takeoverMusic = it },
                    independent = false
                )
                HorizontalDivider(
                    thickness = 1.dp,
                    color = Color(0x40888888),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                StyledToggle(
                    label = "Takeover Call",
                    description = "Take over during an active phone call.",
                    checked = takeoverCall,
                    onCheckedChange = { takeoverCall = it },
                    independent = false
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---- Takeover Phone State ----
            Text(
                text = "Takeover Phone State",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor.copy(alpha = 0.6f),
                ),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundColor, RoundedCornerShape(28.dp))
                    .padding(vertical = 4.dp)
            ) {
                StyledToggle(
                    label = "Takeover Ringing Call",
                    description = "Take over when a call is ringing.",
                    checked = takeoverRingingCall,
                    onCheckedChange = { takeoverRingingCall = it },
                    independent = false
                )
                HorizontalDivider(
                    thickness = 1.dp,
                    color = Color(0x40888888),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                StyledToggle(
                    label = "Takeover Media Start",
                    description = "Take over when media playback starts.",
                    checked = takeoverMediaStart,
                    onCheckedChange = { takeoverMediaStart = it },
                    independent = false
                )
            }

            // ---- Advanced Options ----
            Text(
                text = "Advanced Options",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor.copy(alpha = 0.6f),
                ),
                modifier = Modifier.padding(16.dp, bottom = 2.dp, top = 24.dp)
            )

            Spacer(modifier = Modifier.height(2.dp))

            StyledToggle(
                label = "Use Alternate Head Tracking Packets",
                description = "Use an alternate packet format for head tracking that may improve compatibility with some devices.",
                checked = useAlternateHeadTracking,
                onCheckedChange = { useAlternateHeadTracking = it }
            )

            // ---- Contact ----
            Text(
                text = "Contact",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor.copy(alpha = 0.6f),
                ),
                modifier = Modifier.padding(16.dp, bottom = 2.dp, top = 24.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundColor, RoundedCornerShape(28.dp))
                    .clip(RoundedCornerShape(28.dp))
            ) {
                NavigationButton(
                    to = "",
                    name = "Email",
                    navController = navController,
                    onClick = { /* open email */ },
                    independent = false
                )

                HorizontalDivider(
                    thickness = 1.dp,
                    color = Color(0x40888888),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationButton(
                    to = "",
                    name = "Discord",
                    navController = navController,
                    onClick = { /* open discord */ },
                    independent = false
                )

                HorizontalDivider(
                    thickness = 1.dp,
                    color = Color(0x40888888),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationButton(
                    to = "",
                    name = "GitHub Issues",
                    navController = navController,
                    onClick = { /* open github */ },
                    independent = false
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            NavigationButton(
                to = "open_source_licenses",
                name = "Open Source Licenses",
                navController = navController,
                independent = true
            )

            Spacer(modifier = Modifier.height(bottomPadding))
        }
    }
}
