package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ZoyaForegroundService
import com.example.live.ZoyaState

@Composable
fun TerminalView(
    state: ZoyaState,
    onSendMessage: (String) -> Unit,
    onReconnect: () -> Unit
) {
    val liveSessionManager = ZoyaForegroundService.activeService?.liveSessionManager
    val messages = liveSessionManager?.messages?.collectAsState(initial = emptyList())?.value ?: emptyList()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Terminal Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "NEURAL STREAM",
                    color = Color(0xFF00E5FF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF00E5FF).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${messages.size} entries",
                        color = Color(0xFF00E5FF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (messages.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            liveSessionManager?.clearMessages()
                            Toast.makeText(context, "Log Cleared", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Clear",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onReconnect,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reconnect",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Voice stream is silent",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Speak aloud or type below to interact with X",
                        color = Color.White.copy(alpha = 0.25f),
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages) { message ->
                    TerminalMessageCard(
                        rawMessage = message,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(message))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Direct Text Input Bar
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF131726),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            "Type instruction or command...",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 14.sp
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText.trim())
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank(),
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (inputText.isNotBlank()) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.08f)
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank()) Color.Black else Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
fun TerminalMessageCard(
    rawMessage: String,
    onCopy: () -> Unit
) {
    val isUser = rawMessage.startsWith("You:") || rawMessage.startsWith("user:")
    val isTool = rawMessage.contains("functionResponses") || rawMessage.contains("Calling ") || rawMessage.contains("Opened dialer") || rawMessage.contains("Torch") || rawMessage.contains("Volume") || rawMessage.contains("Brightness")
    val isError = rawMessage.startsWith("Error:") || rawMessage.startsWith("WebSocket Error:") || rawMessage.contains("Missing")

    val cardColor = when {
        isUser -> Color(0xFF24143D)
        isError -> Color(0xFF3B151E)
        isTool -> Color(0xFF14241C)
        else -> Color(0xFF131A2E)
    }

    val borderColor = when {
        isUser -> Color(0xFFB388FF).copy(alpha = 0.5f)
        isError -> Color(0xFFEF5350).copy(alpha = 0.5f)
        isTool -> Color(0xFF00E676).copy(alpha = 0.5f)
        else -> Color(0xFF00E5FF).copy(alpha = 0.3f)
    }

    val tagText = when {
        isUser -> "USER INPUT"
        isError -> "SYSTEM ALERT"
        isTool -> "TOOL EXECUTION"
        else -> "X NEURAL ENGINE"
    }

    val tagColor = when {
        isUser -> Color(0xFFD1C4E9)
        isError -> Color(0xFFFF8A80)
        isTool -> Color(0xFFB9F6CA)
        else -> Color(0xFF80D8FF)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCopy() }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tagText,
                    color = tagColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = rawMessage,
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 14.sp,
                fontFamily = if (isTool) FontFamily.Monospace else FontFamily.Default,
                lineHeight = 20.sp
            )
        }
    }
}
