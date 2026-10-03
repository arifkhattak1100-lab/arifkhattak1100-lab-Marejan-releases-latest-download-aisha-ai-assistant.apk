package com.example.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class AishaAudioPlayer(private val context: Context) : TextToSpeech.OnInitListener {
    companion object {
        private const val TAG = "AishaAudioPlayer"
    }

    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false
    private var amplitudeJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _speechAmplitude = MutableStateFlow(0f)
    val speechAmplitude: StateFlow<Float> = _speechAmplitude.asStateFlow()

    private var onPlaybackFinished: (() -> Unit)? = null

    init {
        try {
            textToSpeech = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing TextToSpeech: ${e.message}", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
            textToSpeech?.let { tts ->
                val result = tts.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "Locale.US not supported, falling back to default")
                    tts.language = Locale.getDefault()
                }

                // Prefer natural female voice profile if available on device
                try {
                    val voices = tts.voices
                    if (!voices.isNullOrEmpty()) {
                        val femaleVoice = voices.firstOrNull { voice ->
                            val name = voice.name.lowercase()
                            (name.contains("female") || name.contains("en-us-x-sfg") || name.contains("en-us-x-tpd")) &&
                                    !voice.isNetworkConnectionRequired
                        } ?: voices.firstOrNull { it.name.lowercase().contains("female") }

                        if (femaleVoice != null) {
                            tts.voice = femaleVoice
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Voice selection info: ${e.message}")
                }

                tts.setPitch(1.15f) // Warm, sweet feminine tone
                tts.setSpeechRate(1.0f)

                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        scope.launch {
                            _isPlaying.value = true
                            startAmplitudeSimulation()
                        }
                    }

                    override fun onDone(utteranceId: String?) {
                        scope.launch {
                            cleanUp()
                            onPlaybackFinished?.invoke()
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        scope.launch {
                            cleanUp()
                            onPlaybackFinished?.invoke()
                        }
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        Log.e(TAG, "TTS Error code: $errorCode")
                        scope.launch {
                            cleanUp()
                            onPlaybackFinished?.invoke()
                        }
                    }
                })
            }
        } else {
            Log.e(TAG, "Failed to initialize TextToSpeech engine with status: $status")
        }
    }

    /**
     * Speaks text using native Android TextToSpeech engine.
     */
    fun speakText(
        text: String,
        pitch: Float = 1.15f,
        speechRate: Float = 1.0f,
        onFinished: (() -> Unit)? = null
    ) {
        stop()
        this.onPlaybackFinished = onFinished

        val tts = textToSpeech
        if (tts == null || !isTtsInitialized) {
            Log.w(TAG, "TTS not ready yet")
            onFinished?.invoke()
            return
        }

        try {
            tts.setPitch(pitch)
            tts.setSpeechRate(speechRate)
            val utteranceId = "aisha_utt_${System.currentTimeMillis()}"
            val result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            if (result == TextToSpeech.ERROR) {
                Log.e(TAG, "TTS speak returned ERROR")
                cleanUp()
                onFinished?.invoke()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during TTS speak: ${e.message}", e)
            cleanUp()
            onFinished?.invoke()
        }
    }

    /**
     * Backward-compatible playAudio stub.
     */
    fun playAudio(audioBytes: ByteArray, onFinished: (() -> Unit)? = null) {
        onFinished?.invoke()
    }

    /**
     * Interrupts Aisha speaking immediately.
     */
    fun stop() {
        amplitudeJob?.cancel()
        _speechAmplitude.value = 0f
        try {
            textToSpeech?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping TTS: ${e.message}")
        }
        _isPlaying.value = false
    }

    private fun cleanUp() {
        amplitudeJob?.cancel()
        _speechAmplitude.value = 0f
        _isPlaying.value = false
    }

    private fun startAmplitudeSimulation() {
        amplitudeJob?.cancel()
        amplitudeJob = scope.launch {
            var phase = 0f
            while (isActive && _isPlaying.value) {
                phase += 0.25f
                val base = (kotlin.math.sin(phase) * 0.4f + 0.6f).toFloat()
                val variation = ((System.currentTimeMillis() % 7) / 10f) * 0.3f
                _speechAmplitude.value = (base + variation).coerceIn(0.2f, 1f)
                delay(60)
            }
            _speechAmplitude.value = 0f
        }
    }

    fun shutdown() {
        stop()
        try {
            textToSpeech?.shutdown()
        } catch (_: Exception) {}
        textToSpeech = null
        isTtsInitialized = false
    }
}
