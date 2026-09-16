package com.example.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.live.ZoyaState

@Composable
fun ConsoleView(
    state: ZoyaState,
    serviceStarted: Boolean,
    stateColor: Color,
    apiKey: String,
    isAccessibilityActive: Boolean,
    onOpenApiKeyDialog: () -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onReconnect: () -> Unit,
    onQuickPrompt: (String) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Real-time Status Capsule
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.05f),
                border = BorderStroke(1.dp, stateColor.copy(alpha = 0.35f)),
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(stateColor)
                    )
                    Text(
                        text = when (state) {
                            ZoyaState.LISTENING -> "VOICE UPLINK ACTIVE • LISTENING"
                            ZoyaState.THINKING -> "NEURAL SYNTHESIS • PROCESSING"
                            ZoyaState.SPEAKING -> "TRANSMITTING RESPONSE"
                            ZoyaState.IDLE -> if (serviceStarted) "CONNECTED • IDLE" else "STANDBY • READY TO ENGAGE"
                        },
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            // Holographic 3D Neon Orb
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (!serviceStarted) onStartService()
                    },
                contentAlignment = Alignment.Center
            ) {
                ZoyaOrb(state = state)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Telemetry Status Badges (Mic, Accessibility, Cellular)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Mic,
                    title = "Microphone",
                    subtitle = if (serviceStarted) "16kHz Live" else "Standby",
                    accent = if (serviceStarted) Color(0xFF00E676) else Color.White.copy(alpha = 0.4f)
                )

                TelemetryCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(intent)
                        },
                    icon = Icons.Default.TouchApp,
                    title = "Automation",
                    subtitle = if (isAccessibilityActive) "Auto-Click On" else "Tap to Enable",
                    accent = if (isAccessibilityActive) Color(0xFF00E5FF) else Color(0xFFFFB74D)
                )

                TelemetryCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Phone,
                    title = "Cellular",
                    subtitle = "SIM Ready",
                    accent = Color(0xFFB388FF)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Quick Voice Command Suggestions
            Text(
                text = "VOICE ACTIONS",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(start = 4.dp, bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionChip("📞 Call Contact") { onQuickPrompt("Call my mom") }
                ActionChip("🔦 Torch On") { onQuickPrompt("Turn on the flashlight") }
                ActionChip("🔊 Volume 80%") { onQuickPrompt("Set volume to 80%") }
                ActionChip("💬 WhatsApp") { onQuickPrompt("Open WhatsApp") }
                ActionChip("☀️ Brightness 100%") { onQuickPrompt("Set brightness to 100") }
                ActionChip("▶️ Play Music") { onQuickPrompt("Play music") }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Main Action Console
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!serviceStarted) {
                if (apiKey.isEmpty()) {
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("setup_api_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E2438),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(28.dp),
                        border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
                        onClick = onOpenApiKeyDialog
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Set Up Gemini API Key",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                } else {
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("start_zoya_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1A1F36),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(28.dp),
                        border = BorderStroke(1.5.dp, Color(0xFFB388FF).copy(alpha = 0.7f)),
                        onClick = onStartService
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = null,
                            tint = Color(0xFFB388FF),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "INITIALIZE X",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            letterSpacing = 1.2.sp,
                            color = Color.White
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onReconnect,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reconnect", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = onStopService,
                        modifier = Modifier
                            .weight(1.2f)
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD32F2F).copy(alpha = 0.25f),
                            contentColor = Color(0xFFFF8A80)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Disconnect", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun TelemetryCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    accent: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.04f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = title,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ActionChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
