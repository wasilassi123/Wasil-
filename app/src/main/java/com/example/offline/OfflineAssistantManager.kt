package com.example.offline

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.live.ZoyaState
import com.example.tools.ToolExecutionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class OfflineAssistantManager(
    private val context: Context,
    private val toolEngine: ToolExecutionEngine,
    private val onMessageLog: (String) -> Unit,
    private val onStateChanged: (ZoyaState) -> Unit
) : TextToSpeech.OnInitListener {

    private val TAG = "OfflineAssistant"
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val parser = OfflineIntentParser(context, toolEngine)

    private val _zoyaState = MutableStateFlow(ZoyaState.IDLE)
    val zoyaState: StateFlow<ZoyaState> = _zoyaState.asStateFlow()

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isAssistantActive = false
    private var shouldListenAfterTts = true

    init {
        initTts()
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TextToSpeech", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.language = Locale.getDefault()
            }
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    updateState(ZoyaState.SPEAKING)
                }

                override fun onDone(utteranceId: String?) {
                    if (isAssistantActive && shouldListenAfterTts) {
                        updateState(ZoyaState.LISTENING)
                        scope.launch(Dispatchers.Main) {
                            delay(250)
                            startListeningInternal()
                        }
                    } else {
                        updateState(ZoyaState.IDLE)
                    }
                }

                override fun onError(utteranceId: String?) {
                    updateState(if (isAssistantActive && shouldListenAfterTts) ZoyaState.LISTENING else ZoyaState.IDLE)
                }
            })
            isTtsReady = true
            Log.i(TAG, "Offline TextToSpeech ready.")
        } else {
            Log.e(TAG, "TTS Initialization failed with status: $status")
        }
    }

    private fun updateState(newState: ZoyaState) {
        _zoyaState.value = newState
        onStateChanged(newState)
    }

    fun startOfflineAssistant() {
        isAssistantActive = true
        shouldListenAfterTts = true
        onMessageLog("[Offline Talking Mode] Real-time voice conversation ready.")
        val welcome = "Hello! I am X, running on-device in offline talking mode. How can I help you today?"
        speak(welcome, keepListeningAfter = true)
    }

    fun stopOfflineAssistant() {
        isAssistantActive = false
        shouldListenAfterTts = false
        stopListening()
        stopSpeaking()
        updateState(ZoyaState.IDLE)
        onMessageLog("[Offline Mode Stopped]")
    }

    fun executeTextCommand(text: String) {
        if (text.isBlank()) return
        onMessageLog("You (Offline): $text")
        updateState(ZoyaState.THINKING)

        scope.launch(Dispatchers.Default) {
            val result = parser.parseAndExecute(text)

            scope.launch(Dispatchers.Main) {
                onMessageLog(result.uiLogMessage)
                if (result.isGoodbye) {
                    speak(result.speechResponse, keepListeningAfter = false)
                    isAssistantActive = false
                } else {
                    speak(result.speechResponse, keepListeningAfter = result.shouldKeepListening)
                }
            }
        }
    }

    fun speak(text: String, keepListeningAfter: Boolean = true) {
        shouldListenAfterTts = keepListeningAfter
        if (!isTtsReady || tts == null) {
            Log.w(TAG, "TTS not ready to speak: $text")
            updateState(if (keepListeningAfter && isAssistantActive) ZoyaState.LISTENING else ZoyaState.IDLE)
            if (keepListeningAfter && isAssistantActive) startListeningInternal()
            return
        }

        updateState(ZoyaState.SPEAKING)
        val utteranceId = "offline_reply_${System.currentTimeMillis()}"
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stopSpeaking() {
        try {
            if (tts?.isSpeaking == true) {
                tts?.stop()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS", e)
        }
    }

    fun triggerListening() {
        stopSpeaking()
        isAssistantActive = true
        shouldListenAfterTts = true
        startListeningInternal()
    }

    fun startListening() {
        if (!isAssistantActive) isAssistantActive = true
        startListeningInternal()
    }

    private fun startListeningInternal() {
        if (isListening) return

        scope.launch(Dispatchers.Main) {
            try {
                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    Log.w(TAG, "Speech recognition not available on this device.")
                    updateState(ZoyaState.IDLE)
                    return@launch
                }

                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            isListening = true
                            updateState(ZoyaState.LISTENING)
                        }

                        override fun onBeginningOfSpeech() {
                            updateState(ZoyaState.LISTENING)
                        }

                        override fun onRmsChanged(rmsdB: Float) {}

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            isListening = false
                            updateState(ZoyaState.THINKING)
                        }

                        override fun onError(error: Int) {
                            isListening = false
                            val errorName = when (error) {
                                SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network unavailable (Using on-device)"
                                else -> "Code $error"
                            }
                            Log.d(TAG, "SpeechRecognizer error: $errorName")
                            if (isAssistantActive && error != SpeechRecognizer.ERROR_CLIENT) {
                                updateState(ZoyaState.IDLE)
                            }
                        }

                        override fun onResults(results: Bundle?) {
                            isListening = false
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val recognizedText = matches?.firstOrNull()
                            if (!recognizedText.isNullOrBlank()) {
                                executeTextCommand(recognizedText)
                            } else {
                                updateState(ZoyaState.IDLE)
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {}

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                }

                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start speech recognition", e)
                isListening = false
                updateState(ZoyaState.IDLE)
            }
        }
    }

    fun stopListening() {
        isListening = false
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun release() {
        isAssistantActive = false
        shouldListenAfterTts = false
        stopListening()
        stopSpeaking()
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {}

        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
        } catch (e: Exception) {}
    }
}
