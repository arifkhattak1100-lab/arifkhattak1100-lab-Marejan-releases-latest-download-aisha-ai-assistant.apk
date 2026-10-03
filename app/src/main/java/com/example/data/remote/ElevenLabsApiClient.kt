package com.example.data.remote

import android.util.Log
import com.example.data.local.AishaPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class ElevenLabsResult {
    data class Success(val audioData: ByteArray) : ElevenLabsResult()
    data class Error(val message: String, val isConfigurationError: Boolean) : ElevenLabsResult()
}

class ElevenLabsApiClient(
    private val preferences: AishaPreferences,
    private val networkMonitor: NetworkMonitor
) {
    companion object {
        private const val TAG = "ElevenLabsApiClient"
        private const val BASE_URL = "https://api.elevenlabs.io/v1"
        const val UNAVAILABLE_MESSAGE =
            "Aisha's voice isn't available right now. Please check the ElevenLabs configuration."
        const val OFFLINE_MESSAGE =
            "You're offline. Aisha will reconnect when your connection is available."
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Synthesizes speech using ElevenLabs API.
     * Implements retry logic: up to 3 times with 1s, 2s, 4s delay.
     * Strictly NO fallback to Android TTS or fake voice.
     */
    suspend fun textToSpeech(text: String): ElevenLabsResult = withContext(Dispatchers.IO) {
        if (!networkMonitor.isOnline()) {
            return@withContext ElevenLabsResult.Error(OFFLINE_MESSAGE, isConfigurationError = false)
        }

        val apiKey = preferences.getElevenLabsApiKey()
        if (apiKey.isBlank()) {
            Log.e(TAG, "ElevenLabs API key is missing")
            return@withContext ElevenLabsResult.Error(UNAVAILABLE_MESSAGE, isConfigurationError = true)
        }

        val voiceId = preferences.getVoiceId()
        val stability = preferences.getStability()
        val similarity = preferences.getSimilarity()

        val jsonBody = JSONObject().apply {
            put("text", text)
            // Use turbo model for low latency voice assistant responses
            put("model_id", "eleven_turbo_v2_5")
            put("voice_settings", JSONObject().apply {
                put("stability", stability)
                put("similarity_boost", similarity)
                put("style", 0.15)
                put("use_speaker_boost", true)
            })
        }.toString()

        val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())
        val url = "$BASE_URL/text-to-speech/$voiceId"

        val retryDelays = longArrayOf(1000L, 2000L, 4000L)
        var lastError: String = UNAVAILABLE_MESSAGE

        for (attempt in 0..retryDelays.size) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .addHeader("xi-api-key", apiKey)
                    .addHeader("Accept", "audio/mpeg")
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body
                        if (body != null) {
                            val bytes = body.bytes()
                            if (bytes.isNotEmpty()) {
                                return@withContext ElevenLabsResult.Success(bytes)
                            }
                        }
                    } else {
                        val code = response.code
                        val errBody = response.body?.string() ?: ""
                        Log.e(TAG, "ElevenLabs HTTP $code error: $errBody")
                        if (code == 401 || code == 403) {
                            // Invalid or unauthorized key - no retry will fix this
                            return@withContext ElevenLabsResult.Error(
                                UNAVAILABLE_MESSAGE,
                                isConfigurationError = true
                            )
                        }
                        lastError = UNAVAILABLE_MESSAGE
                    }
                }
            } catch (e: IOException) {
                Log.w(TAG, "Network attempt $attempt failed: ${e.message}")
                if (!networkMonitor.isOnline()) {
                    return@withContext ElevenLabsResult.Error(OFFLINE_MESSAGE, isConfigurationError = false)
                }
                lastError = UNAVAILABLE_MESSAGE
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error: ${e.message}")
                lastError = UNAVAILABLE_MESSAGE
                break
            }

            if (attempt < retryDelays.size) {
                delay(retryDelays[attempt])
            }
        }

        ElevenLabsResult.Error(lastError, isConfigurationError = false)
    }

    /**
     * Validates connection to ElevenLabs using the user's API key.
     */
    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = preferences.getElevenLabsApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(Exception("ElevenLabs API Key is not set."))
        }

        try {
            val request = Request.Builder()
                .url("$BASE_URL/user")
                .addHeader("xi-api-key", apiKey)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    val subscription = json.optJSONObject("subscription")
                    val characterCount = subscription?.optInt("character_count", 0) ?: 0
                    val characterLimit = subscription?.optInt("character_limit", 10000) ?: 10000
                    Result.success("Connected to ElevenLabs! ($characterCount / $characterLimit chars used)")
                } else {
                    Result.failure(Exception("ElevenLabs auth failed (${response.code}). Check your API Key."))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Connection failed: ${e.localizedMessage ?: "Unknown error"}"))
        }
    }
}
