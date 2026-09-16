package com.example.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.BuildConfig
import com.example.ZoyaForegroundService
import com.example.accessibility.ZoyaAccessibilityService
import com.example.live.ZoyaState

enum class MainTab {
    CONSOLE,
    TERMINAL
}

@Composable
fun ZoyaScreen() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onNavigateToChat = { navController.navigate("chat") }
            )
        }
        composable("chat") {
            ChatScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigateToChat: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ZoyaPrefs", Context.MODE_PRIVATE) }
    val initialKey = remember {
        prefs.getString("api_key", "")?.takeIf { it.isNotEmpty() }
            ?: BuildConfig.GEMINI_API_KEY.takeIf { it.isNotEmpty() && it != "MY_GEMINI_API_KEY" }
            ?: ""
    }
    var apiKey by remember { mutableStateOf(initialKey) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var zoyaState by remember { mutableStateOf(ZoyaForegroundService.currentState) }
    var serviceStarted by remember { mutableStateOf(ZoyaForegroundService.activeService != null) }
    var currentTab by remember { mutableStateOf(MainTab.CONSOLE) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[android.Manifest.permission.RECORD_AUDIO] == true) {
            val intent = Intent(context, ZoyaForegroundService::class.java)
            ContextCompat.startForegroundService(context, intent)
            serviceStarted = true
            Toast.makeText(context, "Voice Uplink Initialized", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Microphone permission is required!", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        ZoyaForegroundService.onStateChange = { state ->
            zoyaState = state
            serviceStarted = (state != ZoyaState.IDLE) || (ZoyaForegroundService.activeService != null)
        }
    }

    val stateColor by animateColorAsState(
        targetValue = when (zoyaState) {
            ZoyaState.LISTENING -> Color(0xFFB388FF)
            ZoyaState.THINKING -> Color(0xFFFFD180)
            ZoyaState.SPEAKING -> Color(0xFF69F0AE)
            ZoyaState.IDLE -> if (serviceStarted) Color(0xFF80D8FF) else Color(0xFF78909C)
        },
        animationSpec = tween(400),
        label = "stateColor"
    )

    val isAccessibilityActive = ZoyaAccessibilityService.instance != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, stateColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "X",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "X ASSISTANT",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    letterSpacing = 1.2.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(stateColor)
                                )
                            }
                            Text(
                                text = "Created by Wasil",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0A0C14).copy(alpha = 0.95f)
                ),
                actions = {
                    IconButton(
                        onClick = { showApiKeyDialog = true },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.07f))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
        },
        containerColor = Color(0xFF08090F)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF14192B),
                            Color(0xFF08090F)
                        ),
                        radius = 1600f
                    )
                )
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Segmented Tab Pill Selector (Console vs Terminal)
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Console Tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (currentTab == MainTab.CONSOLE) stateColor.copy(alpha = 0.2f)
                                    else Color.Transparent
                                )
                                .border(
                                    width = if (currentTab == MainTab.CONSOLE) 1.dp else 0.dp,
                                    color = if (currentTab == MainTab.CONSOLE) stateColor.copy(alpha = 0.5f) else Color.Transparent,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { currentTab = MainTab.CONSOLE }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = "Console",
                                    tint = if (currentTab == MainTab.CONSOLE) Color.White else Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Orb Console",
                                    color = if (currentTab == MainTab.CONSOLE) Color.White else Color.White.copy(alpha = 0.6f),
                                    fontSize = 13.sp,
                                    fontWeight = if (currentTab == MainTab.CONSOLE) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }

                        // Terminal Tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (currentTab == MainTab.TERMINAL) Color(0xFF00E5FF).copy(alpha = 0.18f)
                                    else Color.Transparent
                                )
                                .border(
                                    width = if (currentTab == MainTab.TERMINAL) 1.dp else 0.dp,
                                    color = if (currentTab == MainTab.TERMINAL) Color(0xFF00E5FF).copy(alpha = 0.5f) else Color.Transparent,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { currentTab = MainTab.TERMINAL }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = "Activity Logs",
                                    tint = if (currentTab == MainTab.TERMINAL) Color.White else Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Live Activity",
                                    color = if (currentTab == MainTab.TERMINAL) Color.White else Color.White.copy(alpha = 0.6f),
                                    fontSize = 13.sp,
                                    fontWeight = if (currentTab == MainTab.TERMINAL) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                when (currentTab) {
                    MainTab.CONSOLE -> {
                        ConsoleView(
                            state = zoyaState,
                            serviceStarted = serviceStarted,
                            stateColor = stateColor,
                            apiKey = apiKey,
                            isAccessibilityActive = isAccessibilityActive,
                            onOpenApiKeyDialog = { showApiKeyDialog = true },
                            onStartService = {
                                val hasMic = ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                val hasContacts = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                val hasPhone = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CALL_PHONE) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                val hasPhoneState = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED

                                if (hasMic && hasContacts && hasPhone && hasPhoneState) {
                                    val intent = Intent(context, ZoyaForegroundService::class.java)
                                    ContextCompat.startForegroundService(context, intent)
                                    serviceStarted = true
                                } else {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            android.Manifest.permission.RECORD_AUDIO,
                                            android.Manifest.permission.READ_CONTACTS,
                                            android.Manifest.permission.CALL_PHONE,
                                            android.Manifest.permission.READ_PHONE_STATE
                                        )
                                    )
                                }
                            },
                            onStopService = {
                                val intent = Intent(context, ZoyaForegroundService::class.java)
                                context.stopService(intent)
                                serviceStarted = false
                            },
                            onReconnect = {
                                val service = ZoyaForegroundService.activeService
                                if (service != null) {
                                    service.reconnectSession()
                                } else {
                                    val intent = Intent(context, ZoyaForegroundService::class.java)
                                    ContextCompat.startForegroundService(context, intent)
                                }
                            },
                            onQuickPrompt = { prompt ->
                                val service = ZoyaForegroundService.activeService
                                if (service != null) {
                                    service.sendTextMessage(prompt)
                                    Toast.makeText(context, "Command dispatched to X", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Start Voice Uplink to execute commands", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                    MainTab.TERMINAL -> {
                        TerminalView(
                            state = zoyaState,
                            onSendMessage = { text ->
                                val service = ZoyaForegroundService.activeService
                                if (service != null) {
                                    service.sendTextMessage(text)
                                } else {
                                    Toast.makeText(context, "Please start X first", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onReconnect = {
                                ZoyaForegroundService.activeService?.reconnectSession()
                            }
                        )
                    }
                }
            }
        }
    }

    if (showApiKeyDialog) {
        ApiKeySettingsDialog(
            currentKey = apiKey,
            onDismiss = { showApiKeyDialog = false },
            onSave = { newKey ->
                prefs.edit().putString("api_key", newKey).apply()
                apiKey = newKey
                showApiKeyDialog = false
                Toast.makeText(context, "API Key Saved", Toast.LENGTH_SHORT).show()
            },
            onOpenAccessibility = {
                val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            }
        )
    }
}

@Composable
fun ChatScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        containerColor = Color(0xFF08090F),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Live Activity Stream",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            TerminalView(
                state = ZoyaForegroundService.currentState,
                onSendMessage = { text ->
                    ZoyaForegroundService.activeService?.sendTextMessage(text)
                },
                onReconnect = {
                    ZoyaForegroundService.activeService?.reconnectSession()
                }
            )
        }
    }
}
