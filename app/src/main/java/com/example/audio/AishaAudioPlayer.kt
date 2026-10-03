package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
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
import java.io.File
import java.io.FileOutputStream

class AishaAudioPlayer(private val context: Context) {
    companion object {
        private const val TAG = "AishaAudioPlayer"
    }

    private var mediaPlayer: MediaPlayer? = null
    private var amplitudeJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _speechAmplitude = MutableStateFlow(0f)
    val speechAmplitude: StateFlow<Float> = _speechAmplitude.asStateFlow()

    private var onPlaybackFinished: (() -> Unit)? = null

    /**
     * Plays the audio data returned directly from ElevenLabs TTS.
     */
    fun playAudio(audioBytes: ByteArray, onFinished: (() -> Unit)? = null) {
        stop()
        this.onPlaybackFinished = onFinished

        try {
            // Write audio bytes to temporary cache file
            val tempFile = File.createTempFile("aisha_voice_", ".mp3", context.cacheDir)
            FileOutputStream(tempFile).use { fos ->
                fos.write(audioBytes)
                fos.flush()
            }

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .build()
                )
                setDataSource(tempFile.absolutePath)
                setOnPreparedListener { mp ->
                    mp.start()
                    _isPlaying.value = true
                    startAmplitudeSimulation()
                }
                setOnCompletionListener {
                    cleanUp()
                    try {
                        tempFile.delete()
                    } catch (_: Exception) {}
                    onPlaybackFinished?.invoke()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                    cleanUp()
                    try {
                        tempFile.delete()
                    } catch (_: Exception) {}
                    onPlaybackFinished?.invoke()
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio playback: ${e.message}", e)
            cleanUp()
            onPlaybackFinished?.invoke()
        }
    }

    /**
     * Interrupts Aisha speaking immediately.
     */
    fun stop() {
        amplitudeJob?.cancel()
        _speechAmplitude.value = 0f
        mediaPlayer?.let { mp ->
            try {
                if (mp.isPlaying) {
                    mp.stop()
                }
                mp.reset()
                mp.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping media player: ${e.message}")
            }
        }
        mediaPlayer = null
        _isPlaying.value = false
    }

    private fun cleanUp() {
        amplitudeJob?.cancel()
        _speechAmplitude.value = 0f
        _isPlaying.value = false
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun startAmplitudeSimulation() {
        amplitudeJob?.cancel()
        amplitudeJob = scope.launch {
            var phase = 0f
            while (isActive && _isPlaying.value) {
                phase += 0.25f
                // Dynamic undulating voice amplitude for orb speaking visualization
                val base = (kotlin.math.sin(phase) * 0.4f + 0.6f).toFloat()
                val variation = ((System.currentTimeMillis() % 7) / 10f) * 0.3f
                _speechAmplitude.value = (base + variation).coerceIn(0.2f, 1f)
                delay(60)
            }
            _speechAmplitude.value = 0f
        }
    }
}
