package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.live.LiveSessionManager
import com.example.live.ZoyaState
import com.example.offline.NetworkMonitor
import com.example.offline.OfflineAssistantManager
import com.example.tools.ToolExecutionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class AssistantMode {
    AUTO,
    OFFLINE,
    ONLINE
}

class ZoyaForegroundService : Service() {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null

    lateinit var liveSessionManager: LiveSessionManager
    lateinit var offlineAssistantManager: OfflineAssistantManager
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var toolEngine: ToolExecutionEngine

    private var isRecording = false

    // Configuration for Gemini Live Audio (16kHz, Mono, PCM 16-bit)
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 4

    // Configuration for audio output (24kHz, Mono, PCM 16-bit is typical for server output)
    private val outputSampleRate = 24000
    private val outChannelConfig = AudioFormat.CHANNEL_OUT_MONO
    private val outBufferSize = AudioTrack.getMinBufferSize(outputSampleRate, outChannelConfig, audioFormat) * 4

    companion object {
        var currentState: ZoyaState = ZoyaState.IDLE
            private set
        var onStateChange: ((ZoyaState) -> Unit)? = null

        private val _messages = kotlinx.coroutines.flow.MutableStateFlow<List<String>>(emptyList())
        val messages: kotlinx.coroutines.flow.StateFlow<List<String>> = _messages.asStateFlow()

        private val _assistantMode = kotlinx.coroutines.flow.MutableStateFlow(AssistantMode.AUTO)
        val assistantMode: kotlinx.coroutines.flow.StateFlow<AssistantMode> = _assistantMode.asStateFlow()

        private val _isOfflineModeActive = kotlinx.coroutines.flow.MutableStateFlow(false)
        val isOfflineModeActive: kotlinx.coroutines.flow.StateFlow<Boolean> = _isOfflineModeActive.asStateFlow()

        // Provide a way to send message from UI to active service if it exists
        var activeService: ZoyaForegroundService? = null
    }

    override fun onCreate() {
        super.onCreate()
        try {
            activeService = this
            toolEngine = ToolExecutionEngine(this)
            networkMonitor = NetworkMonitor(this)
            
            val onAudioOut: (ByteArray) -> Unit = { audioData ->
                playAudio(audioData)
            }
            
            val onInterruptOut: () -> Unit = {
                try {
                    audioOutputQueue.clear()
                    if (audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        audioTrack?.pause()
                        audioTrack?.flush()
                        audioTrack?.play()
                    }
                } catch (e: Exception) {
                    Log.e("ZoyaDiagnostic", "Error flushing track on interrupt", e)
                }
            }
            
            liveSessionManager = LiveSessionManager(this, toolEngine, onAudioOut, onInterruptOut)
            
            offlineAssistantManager = OfflineAssistantManager(
                context = this,
                toolEngine = toolEngine,
                onMessageLog = { msg ->
                    liveSessionManager.addMessage(msg)
                },
                onStateChanged = { state ->
                    if (_isOfflineModeActive.value) {
                        currentState = state
                        onStateChange?.invoke(state)
                    }
                }
            )

            liveSessionManager.onConnectionFailed = {
                if (_assistantMode.value == AssistantMode.AUTO) {
                    scope.launch(Dispatchers.Main) {
                        liveSessionManager.addMessage("[Auto-Fallback] Switched to On-Device Offline Mode.")
                        switchToOffline()
                    }
                }
            }

            createNotificationChannel()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    startForeground(1, createNotification(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
                } catch (e: Exception) {
                    try { startForeground(1, createNotification()) } catch(e: Exception) { }
                }
            } else {
                try { startForeground(1, createNotification()) } catch(e: Exception) { }
            }

            scope.launch {
                liveSessionManager.zoyaState.collect { state ->
                    if (!_isOfflineModeActive.value) {
                        currentState = state
                        onStateChange?.invoke(state)
                    }
                }
            }
            scope.launch {
                liveSessionManager.messages.collect { msgList ->
                    _messages.value = msgList
                }
            }

            scope.launch {
                networkMonitor.isOnline.collect { online ->
                    if (_assistantMode.value == AssistantMode.AUTO) {
                        if (online && _isOfflineModeActive.value) {
                            liveSessionManager.addMessage("[Network Restored] Switching to Cloud AI.")
                            switchToOnline()
                        } else if (!online && !_isOfflineModeActive.value) {
                            liveSessionManager.addMessage("[Network Lost] Switching to On-Device Offline Mode.")
                            switchToOffline()
                        }
                    }
                }
            }

            initAudioTrack()
            
            val shouldStartOffline = _assistantMode.value == AssistantMode.OFFLINE ||
                    (_assistantMode.value == AssistantMode.AUTO && !networkMonitor.isOnline.value)
            
            if (shouldStartOffline) {
                switchToOffline()
            } else {
                switchToOnline()
            }
        } catch (e: Exception) {
            Log.e("ZoyaService", "Error in onCreate", e)
        }
    }

    private val audioOutputQueue = java.util.concurrent.LinkedBlockingQueue<ByteArray>()
    private var isAudioPlaybackActive = false

    private fun startAudioPlaybackLoop() {
        isAudioPlaybackActive = true
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            while (isActive && isAudioPlaybackActive) {
                try {
                    val data = audioOutputQueue.poll(50, java.util.concurrent.TimeUnit.MILLISECONDS)
                    if (data != null) {
                        if (audioTrack?.playState != AudioTrack.PLAYSTATE_PLAYING) {
                            audioTrack?.play()
                        }
                        audioTrack?.write(data, 0, data.size)
                    }
                } catch (e: Exception) {
                    Log.e("ZoyaDiagnostic", "Playback loop error", e)
                }
            }
        }
    }

    private fun initAudioTrack() {
        try {
            val minBuf = AudioTrack.getMinBufferSize(outputSampleRate, outChannelConfig, audioFormat)
            val finalBuf = if (minBuf > 0) minBuf * 4 else 8192
            
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(outputSampleRate)
                        .setChannelMask(outChannelConfig)
                        .build()
                )
                .setBufferSizeInBytes(finalBuf)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
                
            audioTrack?.play()
            startAudioPlaybackLoop()
        } catch (e: Exception) {
            Log.e("ZoyaService", "Error initializing AudioTrack", e)
        }
    }

    private fun playAudio(data: ByteArray) {
        try {
            audioOutputQueue.offer(data)
        } catch (e: Exception) {
            Log.e("ZoyaDiagnostic", "Error queueing audio", e)
        }
    }

    private fun startMicrophoneLoop() {
        if (isRecording && audioRecord != null) return

        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.e("ZoyaDiagnostic", "Missing RECORD_AUDIO permission")
            return
        }

        try {
            val minBuf = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val finalBuf = if (minBuf > 0) minBuf * 4 else 8192
            
            Log.i("ZoyaDiagnostic", "Starting microphone recording. bufSize=$finalBuf")

            val ctx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                createAttributionContext("zoya_audio")
            } else {
                this
            }
            audioRecord = AudioRecord.Builder()
                .setContext(ctx)
                .setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .setEncoding(audioFormat)
                        .build()
                )
                .setBufferSizeInBytes(finalBuf)
                .build()

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("ZoyaDiagnostic", "AudioRecord initialization failed!")
                return
            }

            audioRecord?.startRecording()
            isRecording = true

            scope.launch(Dispatchers.IO) {
                val chunkSize = 1600
                val audioBuffer = ShortArray(chunkSize)
                var readCount = 0
                while (isActive && isRecording) {
                    try {
                        val readResult = audioRecord?.read(audioBuffer, 0, chunkSize) ?: 0
                        if (readResult > 0) {
                            if (readCount % 50 == 0) {
                                Log.v("ZoyaDiagnostic", "Microphone read loop active. readResult=$readResult")
                            }
                            readCount++
                            processAudio(audioBuffer, readResult)
                        } else {
                            Log.e("ZoyaDiagnostic", "Microphone read failed or empty: $readResult")
                            delay(50)
                        }
                    } catch (e: Exception) {
                        Log.e("ZoyaDiagnostic", "Error reading audio", e)
                        delay(50)
                    }
                }
                Log.i("ZoyaDiagnostic", "Microphone loop stopped.")
            }
        } catch (e: Exception) {
            Log.e("ZoyaDiagnostic", "Error starting microphone", e)
        }
    }

    private fun pauseMicrophoneLoop() {
        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            Log.e("ZoyaDiagnostic", "Error pausing microphone", e)
        }
    }

    fun setMode(mode: AssistantMode) {
        _assistantMode.value = mode
        when (mode) {
            AssistantMode.OFFLINE -> {
                switchToOffline()
            }
            AssistantMode.ONLINE -> {
                switchToOnline()
            }
            AssistantMode.AUTO -> {
                if (networkMonitor.isOnline.value) {
                    switchToOnline()
                } else {
                    switchToOffline()
                }
            }
        }
    }

    fun switchToOffline() {
        _isOfflineModeActive.value = true
        liveSessionManager.stopSession()
        pauseMicrophoneLoop()
        offlineAssistantManager.startOfflineAssistant()
        updateNotification("X (Offline Mode)", "On-device AI assistant active.")
    }

    fun switchToOnline() {
        _isOfflineModeActive.value = false
        offlineAssistantManager.stopOfflineAssistant()
        startMicrophoneLoop()
        liveSessionManager.startSession()
        updateNotification("X (Online Mode)", "Voice uplink active.")
    }

    fun triggerListening() {
        if (_isOfflineModeActive.value) {
            offlineAssistantManager.startListening()
        }
    }

    private var consecutiveLoudChunks = 0
    private var lastFlushTime = 0L

    private fun processAudio(buffer: ShortArray, length: Int) {
        if (_isOfflineModeActive.value) return
        val state = liveSessionManager.zoyaState.value
        
        if (state != ZoyaState.IDLE) {
            liveSessionManager.sendAudioData(buffer, length)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    fun sendTextMessage(text: String) {
        if (_isOfflineModeActive.value) {
            offlineAssistantManager.executeTextCommand(text)
        } else {
            liveSessionManager.sendTextMessage(text)
        }
    }

    fun reconnectSession() {
        if (_isOfflineModeActive.value) {
            offlineAssistantManager.stopOfflineAssistant()
            offlineAssistantManager.startOfflineAssistant()
        } else {
            liveSessionManager.reconnect()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activeService = null
        isRecording = false
        isAudioPlaybackActive = false
        currentState = ZoyaState.IDLE
        onStateChange?.invoke(currentState)
        audioOutputQueue.clear()
        try { networkMonitor.unregister() } catch (e: Exception) {}
        try { offlineAssistantManager.release() } catch (e: Exception) {}
        try { audioRecord?.stop() } catch (e: Exception) {}
        try { audioRecord?.release() } catch (e: Exception) {}
        try { audioTrack?.stop() } catch (e: Exception) {}
        try { audioTrack?.release() } catch (e: Exception) {}
        try { liveSessionManager.stopSession() } catch (e: Exception) {}
        job.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "ZOYA_CHANNEL",
                "X Assistant Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        val title = if (_isOfflineModeActive.value) "X (Offline Mode)" else "X is listening..."
        val content = if (_isOfflineModeActive.value) "On-device AI assistant active." else "Background assistant active."
        return NotificationCompat.Builder(this, "ZOYA_CHANNEL")
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(title: String, text: String) {
        try {
            val manager = getSystemService(NotificationManager::class.java)
            val notif = NotificationCompat.Builder(this, "ZOYA_CHANNEL")
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
            manager?.notify(1, notif)
        } catch (e: Exception) {
            // Ignore
        }
    }
}
