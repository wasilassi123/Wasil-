package com.example.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.AssistantMode
import com.example.live.ZoyaState

@Composable
fun ConsoleView(
    state: ZoyaState,
    serviceStarted: Boolean,
    stateColor: Color,
    apiKey: String,
    isAccessibilityActive: Boolean,
    isOfflineMode: Boolean,
    assistantMode: AssistantMode,
    onModeChange: (AssistantMode) -> Unit,
    onOrbClick: () -> Unit,
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
            // Mode Selector Pill (Auto, Offline, Online)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.05f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ModePillItem(
                        label = "Auto",
                        subLabel = "Smart Fallback",
                        icon = Icons.Default.SyncAlt,
                        isSelected = assistantMode == AssistantMode.AUTO,
                        activeColor = Color(0xFF00E5FF),
                        modifier = Modifier.weight(1f),
                        onClick = { onModeChange(AssistantMode.AUTO) }
                    )
                    ModePillItem(
                        label = "Offline",
                        subLabel = "100% On-Device",
                        icon = Icons.Default.SignalCellularConnectedNoInternet0Bar,
                        isSelected = assistantMode == AssistantMode.OFFLINE,
                        activeColor = Color(0xFFFFB74D),
                        modifier = Modifier.weight(1f),
                        onClick = { onModeChange(AssistantMode.OFFLINE) }
                    )
                    ModePillItem(
                        label = "Online",
                        subLabel = "Gemini Live",
                        icon = Icons.Default.CloudQueue,
                        isSelected = assistantMode == AssistantMode.ONLINE,
                        activeColor = Color(0xFFB388FF),
                        modifier = Modifier.weight(1f),
                        onClick = { onModeChange(AssistantMode.ONLINE) }
                    )
                }
            }

            // Real-time Status Capsule
            val capsuleColor = if (isOfflineMode) Color(0xFFFFB74D) else stateColor
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.05f),
                border = BorderStroke(1.dp, capsuleColor.copy(alpha = 0.4f)),
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
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
                            .background(capsuleColor)
                    )
                    Text(
                        text = if (isOfflineMode) {
                            when (state) {
                                ZoyaState.LISTENING -> "OFFLINE • LISTENING ON-DEVICE"
                                ZoyaState.THINKING -> "OFFLINE • EXECUTING TOOL"
                                ZoyaState.SPEAKING -> "OFFLINE • SPEAKING (TTS)"
                                ZoyaState.IDLE -> if (serviceStarted) "OFFLINE ENGINE ACTIVE • ON-DEVICE READY" else "OFFLINE STANDBY • READY"
                            }
                        } else {
                            when (state) {
                                ZoyaState.LISTENING -> "VOICE UPLINK ACTIVE • LISTENING"
                                ZoyaState.THINKING -> "NEURAL SYNTHESIS • PROCESSING"
                                ZoyaState.SPEAKING -> "TRANSMITTING RESPONSE"
                                ZoyaState.IDLE -> if (serviceStarted) "CONNECTED • IDLE" else "STANDBY • READY TO ENGAGE"
                            }
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
                    .padding(vertical = 8.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (!serviceStarted) {
                            onStartService()
                        } else {
                            onOrbClick()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                ZoyaOrb(state = state)
            }

            if (serviceStarted && isOfflineMode) {
                Text(
                    text = "Tap orb to start offline voice talking",
                    color = Color(0xFFFFD180),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Telemetry Status Badges (Engine, Automation, Hardware)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryCard(
                    modifier = Modifier.weight(1f),
                    icon = if (isOfflineMode) Icons.Default.Dns else Icons.Default.CloudDone,
                    title = "Engine",
                    subtitle = if (isOfflineMode) "On-Device" else "Gemini Live",
                    accent = if (isOfflineMode) Color(0xFFFFB74D) else Color(0xFF00E5FF)
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
                    accent = if (isAccessibilityActive) Color(0xFF00E676) else Color(0xFFFF9100)
                )

                TelemetryCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.FlashlightOn,
                    title = "Hardware",
                    subtitle = "Tools Ready",
                    accent = Color(0xFFB388FF)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Voice Command Suggestions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isOfflineMode) "OFFLINE TALKING & ACTIONS" else "VOICE ACTIONS",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                if (isOfflineMode) {
                    Text(
                        text = "100% On-Device Talk",
                        color = Color(0xFFFFB74D),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isOfflineMode) {
                    ActionChip("🗣️ Hello X") { onQuickPrompt("Hello X, how are you doing?") }
                    ActionChip("😂 Tell Joke") { onQuickPrompt("Tell me a joke") }
                    ActionChip("🧠 Riddle") { onQuickPrompt("Tell me a riddle") }
                    ActionChip("✨ Motivate Me") { onQuickPrompt("Motivate me") }
                    ActionChip("⚡ Fun Fact") { onQuickPrompt("Tell me an interesting fact") }
                    ActionChip("🎵 Sing Song") { onQuickPrompt("Sing a song for me") }
                    ActionChip("🇮🇳 Kaise ho X") { onQuickPrompt("Kaise ho X") }
                    ActionChip("💡 Who made you?") { onQuickPrompt("Who created you?") }
                    ActionChip("🔦 Torch On") { onQuickPrompt("Turn on flashlight") }
                    ActionChip("🔦 Torch Off") { onQuickPrompt("Turn off flashlight") }
                    ActionChip("🔋 Battery %") { onQuickPrompt("What is the battery level?") }
                    ActionChip("⏰ Time") { onQuickPrompt("What time is it?") }
                    ActionChip("📞 Call Contact") { onQuickPrompt("Call mom") }
                    ActionChip("💬 Open WhatsApp") { onQuickPrompt("Open WhatsApp") }
                    ActionChip("🔊 Volume 50%") { onQuickPrompt("Set volume to 50 percent") }
                    ActionChip("❓ Help") { onQuickPrompt("What can you do offline?") }
                } else {
                    ActionChip("🔦 Torch On") { onQuickPrompt("Turn on flashlight") }
                    ActionChip("🔦 Torch Off") { onQuickPrompt("Turn off flashlight") }
                    ActionChip("🔋 Battery %") { onQuickPrompt("What is the battery level?") }
                    ActionChip("⏰ Time") { onQuickPrompt("What time is it?") }
                    ActionChip("📞 Call Mom") { onQuickPrompt("Call mom") }
                    ActionChip("💬 Open WhatsApp") { onQuickPrompt("Open WhatsApp") }
                    ActionChip("💡 Who made you?") { onQuickPrompt("Who created you?") }
                    ActionChip("🎵 Play Music") { onQuickPrompt("Play some upbeat songs on YouTube") }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Main Action Console
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!serviceStarted) {
                val isOfflineSelected = assistantMode == AssistantMode.OFFLINE

                if (isOfflineSelected) {
                    // Direct offline activation without needing any API key!
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("start_zoya_offline_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E2412),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(28.dp),
                        border = BorderStroke(1.5.dp, Color(0xFFFFB74D).copy(alpha = 0.8f)),
                        onClick = onStartService
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = Color(0xFFFFB74D),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "START OFFLINE X (ON-DEVICE)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 1.0.sp,
                            color = Color(0xFFFFD180)
                        )
                    }
                } else if (apiKey.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("start_zoya_offline_fallback"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2E2412),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(26.dp),
                            border = BorderStroke(1.dp, Color(0xFFFFB74D).copy(alpha = 0.7f)),
                            onClick = {
                                onModeChange(AssistantMode.OFFLINE)
                                onStartService()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = null,
                                tint = Color(0xFFFFB74D),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Start in Offline Mode (No Key Needed)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = Color(0xFFFFD180)
                            )
                        }

                        OutlinedButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("setup_api_button"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF00E5FF)
                            ),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f)),
                            onClick = onOpenApiKeyDialog
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Set Up Gemini API Key for Online Mode",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
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
                        Text(
                            if (isOfflineMode) "Restart Engine" else "Reconnect",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = onStopService,
                        modifier = Modifier
                            .weight(1.1f)
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
fun ModePillItem(
    label: String,
    subLabel: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) activeColor.copy(alpha = 0.18f)
                else Color.Transparent
            )
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = if (isSelected) activeColor.copy(alpha = 0.6f) else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) activeColor else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = label,
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
            Text(
                text = subLabel,
                color = if (isSelected) activeColor.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.35f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Normal
            )
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
