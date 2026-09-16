package com.example.offline

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.example.tools.ToolExecutionEngine
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class OfflineExecutionResult(
    val speechResponse: String,
    val uiLogMessage: String,
    val toolResult: String? = null,
    val shouldKeepListening: Boolean = true,
    val isGoodbye: Boolean = false
)

class OfflineIntentParser(
    private val context: Context,
    private val toolEngine: ToolExecutionEngine
) {
    private val chatEngine = OfflineChatEngine()

    private suspend fun executeTool(name: String, args: Map<String, Any> = emptyMap()): String {
        val json = buildJsonObject {
            args.forEach { (k, v) ->
                when (v) {
                    is Number -> put(k, JsonPrimitive(v))
                    is Boolean -> put(k, JsonPrimitive(v))
                    else -> put(k, JsonPrimitive(v.toString()))
                }
            }
        }
        return toolEngine.execute(name, json)
    }

    suspend fun parseAndExecute(input: String): OfflineExecutionResult {
        val trimmed = input.trim()
        val lower = trimmed.lowercase(Locale.ROOT)

        // 1. First, check conversational dialogue / chit-chat / humor / greetings / personality / goodbyes
        val chatResult = chatEngine.handleConversationalInput(lower, trimmed)
        if (chatResult != null) {
            return chatResult
        }

        // 2. Help & Capabilities
        if (lower == "help" || lower.contains("what can you do") || lower.contains("commands") || lower.contains("kya kar sakte ho")) {
            val reply = "In offline mode, we can talk, share jokes, riddles, quotes, or I can toggle flashlight, control volume, open apps, make calls, and check battery."
            return OfflineExecutionResult(
                speechResponse = reply,
                uiLogMessage = "[Offline] Help: Offline Talking, Apps, Calls, Torch, Volume, Brightness, Notifications, Battery, Time",
                shouldKeepListening = true
            )
        }

        // 3. Torch / Flashlight
        if (lower.contains("torch") || lower.contains("flashlight")) {
            val state = if (lower.contains("off") || lower.contains("band") || lower.contains("disable")) "off" else "on"
            val res = executeTool("toggleTorch", mapOf("state" to state))
            val speech = if (state == "on") "Flashlight turned on." else "Flashlight turned off."
            return OfflineExecutionResult(
                speechResponse = speech,
                uiLogMessage = "[Offline] Torch: $res",
                toolResult = res,
                shouldKeepListening = true
            )
        }

        // 4. Battery Level
        if (lower.contains("battery") || lower.contains("charge") || lower.contains("charging")) {
            val batteryStatus = getBatteryStatus()
            return OfflineExecutionResult(
                speechResponse = batteryStatus,
                uiLogMessage = "[Offline] $batteryStatus",
                shouldKeepListening = true
            )
        }

        // 5. Time & Date
        if (lower.contains("time") || lower.contains("samay") || lower.contains("waqt")) {
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            val timeStr = timeFormat.format(Date())
            val speech = "The time is $timeStr."
            return OfflineExecutionResult(
                speechResponse = speech,
                uiLogMessage = "[Offline] $speech",
                shouldKeepListening = true
            )
        }
        if (lower.contains("date") || lower.contains("today") || lower.contains("tareekh")) {
            val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
            val dateStr = dateFormat.format(Date())
            val speech = "Today is $dateStr."
            return OfflineExecutionResult(
                speechResponse = speech,
                uiLogMessage = "[Offline] $speech",
                shouldKeepListening = true
            )
        }

        // 6. Volume Control
        if (lower.contains("volume") || lower.contains("sound") || lower.contains("awaz")) {
            if (lower.contains("mute") || lower.contains("silent") || lower.contains("zero")) {
                val res = executeTool("setVolumePercent", mapOf("percent" to "0"))
                return OfflineExecutionResult("Muted volume.", "[Offline] $res", res, shouldKeepListening = true)
            }
            if (lower.contains("max") || lower.contains("full") || lower.contains("100")) {
                val res = executeTool("setVolumePercent", mapOf("percent" to "100"))
                return OfflineExecutionResult("Volume set to maximum.", "[Offline] $res", res, shouldKeepListening = true)
            }
            val percentMatch = Regex("\\b(\\d{1,3})\\s*(?:%|percent)?\\b").find(lower)
            if (percentMatch != null) {
                val num = percentMatch.groupValues[1].toIntOrNull()
                if (num != null && num in 0..100) {
                    val res = executeTool("setVolumePercent", mapOf("percent" to num.toString()))
                    return OfflineExecutionResult("Volume set to $num percent.", "[Offline] $res", res, shouldKeepListening = true)
                }
            }
            if (lower.contains("down") || lower.contains("kam") || lower.contains("lower") || lower.contains("decrease")) {
                val res = executeTool("adjustVolume", mapOf("direction" to "down"))
                return OfflineExecutionResult("Volume decreased.", "[Offline] $res", res, shouldKeepListening = true)
            }
            if (lower.contains("up") || lower.contains("badhao") || lower.contains("increase") || lower.contains("raise")) {
                val res = executeTool("adjustVolume", mapOf("direction" to "up"))
                return OfflineExecutionResult("Volume increased.", "[Offline] $res", res, shouldKeepListening = true)
            }
        }

        // 7. Brightness Control
        if (lower.contains("brightness") || lower.contains("screen light")) {
            val numMatch = Regex("\\b(\\d{1,3})\\s*(?:%|percent)?\\b").find(lower)
            val level = numMatch?.groupValues?.get(1)?.toIntOrNull() ?: if (lower.contains("low")) 20 else if (lower.contains("max") || lower.contains("full")) 100 else 60
            val res = executeTool("setBrightness", mapOf("level" to level.toString()))
            return OfflineExecutionResult("Brightness set to $level percent.", "[Offline] $res", res, shouldKeepListening = true)
        }

        // 8. Notifications & Quick Settings
        if (lower.contains("notification") || lower.contains("notifications")) {
            val res = executeTool("openNotificationPanel")
            return OfflineExecutionResult("Opening notifications.", "[Offline] $res", res, shouldKeepListening = false)
        }
        if (lower.contains("quick settings") || lower.contains("settings panel") || lower.contains("control center")) {
            val res = executeTool("openQuickSettings")
            return OfflineExecutionResult("Opening quick settings.", "[Offline] $res", res, shouldKeepListening = false)
        }

        // 9. Phone Calling
        if (lower.startsWith("call ") || lower.startsWith("dial ") || lower.contains("call to ") || lower.contains("phone ")) {
            val cleanedTarget = trimmed
                .replace(Regex("^(?i)(please\\s+)?(call|dial|phone|make a call to|call to)\\s+"), "")
                .replace(Regex("(?i)\\s+(on sim|using sim|via sim)\\s*(\\d)"), "")
                .trim()

            if (cleanedTarget.isNotEmpty()) {
                val res = executeTool(
                    "searchAndCallContact",
                    mapOf(
                        "contactName" to cleanedTarget,
                        "useDialer" to "false"
                    )
                )
                val speech = "Calling $cleanedTarget."
                return OfflineExecutionResult(speech, "[Offline] Call: $res", res, shouldKeepListening = false)
            }
        }

        // 10. WhatsApp Messaging
        if (lower.startsWith("whatsapp ") || lower.contains("send whatsapp") || lower.contains("whatsapp message")) {
            val afterCommand = trimmed
                .replace(Regex("^(?i)(please\\s+)?(send\\s+)?whatsapp(\\s+message)?(\\s+to)?\\s+"), "")
                .trim()

            val sayingSplit = afterCommand.split(Regex("(?i)\\s+(saying|that|message)\\s+"), limit = 2)
            val recipient = sayingSplit.firstOrNull()?.trim().orEmpty()
            val message = if (sayingSplit.size > 1) sayingSplit[1].trim() else "Hello"

            if (recipient.isNotEmpty()) {
                val res = executeTool(
                    "sendWhatsAppMessage",
                    mapOf(
                        "contactName" to recipient,
                        "message" to message
                    )
                )
                return OfflineExecutionResult("Sending WhatsApp message to $recipient.", "[Offline] WhatsApp: $res", res, shouldKeepListening = false)
            }
        }

        // 11. Play Music / Media
        if (lower.startsWith("play ") || lower.contains("play music") || lower.contains("play song")) {
            val query = trimmed.replace(Regex("^(?i)(please\\s+)?play\\s+"), "").trim()
            val res = executeTool("searchYouTube", mapOf("query" to query))
            return OfflineExecutionResult("Playing $query.", "[Offline] YouTube: $res", res, shouldKeepListening = false)
        }

        // 12. App Launching (e.g., "Open YouTube", "Launch WhatsApp", "Start Camera", "Open Settings")
        if (lower.startsWith("open ") || lower.startsWith("launch ") || lower.startsWith("start ") || lower.contains("kholo")) {
            val appName = trimmed
                .replace(Regex("^(?i)(please\\s+)?(open|launch|start)\\s+"), "")
                .replace(Regex("(?i)\\s+(app|application)$"), "")
                .trim()

            if (appName.isNotEmpty()) {
                val res = executeTool("openApp", mapOf("packageName" to appName))
                val speech = "Opening $appName."
                return OfflineExecutionResult(speech, "[Offline] Open App: $res", res, shouldKeepListening = false)
            }
        }

        // 13. Direct single-word app match
        val commonApps = listOf("camera", "whatsapp", "youtube", "settings", "calculator", "gallery", "chrome", "clock", "spotify", "phone", "maps")
        if (commonApps.contains(lower)) {
            val res = executeTool("openApp", mapOf("packageName" to trimmed))
            return OfflineExecutionResult("Opening $trimmed.", "[Offline] Open App: $res", res, shouldKeepListening = false)
        }

        // Default conversational fallback
        val fallback = listOf(
            "I'm listening in offline talking mode! Feel free to chat with me, ask for a joke, hear a riddle or quote, or ask me to control phone tools.",
            "Main offline mode me sun raha hoon. Aap mujhse baatein kar sakte hain, joke sun sakte hain, ya device control kar sakte hain.",
            "I heard '$trimmed'. In offline mode, you can chat with me, ask questions about Wasil, hear fun facts, or control your phone settings."
        ).random()
        return OfflineExecutionResult(
            speechResponse = fallback,
            uiLogMessage = "[Offline Talking] $fallback",
            shouldKeepListening = true
        )
    }

    private fun getBatteryStatus(): String {
        return try {
            val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, ifilter)
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else level
            if (batteryPct >= 0) {
                val chargingText = if (isCharging) "and currently charging" else "and not charging"
                "Battery is at $batteryPct percent, $chargingText."
            } else {
                "Unable to read battery level."
            }
        } catch (e: Exception) {
            "Unable to retrieve battery status."
        }
    }
}
