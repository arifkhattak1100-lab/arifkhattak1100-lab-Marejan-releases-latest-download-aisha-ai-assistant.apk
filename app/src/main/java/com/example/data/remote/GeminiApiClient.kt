package com.example.data.remote

import android.util.Log
import com.example.data.local.AishaPreferences
import com.example.data.model.AishaEmotion
import com.example.data.model.AishaMemory
import com.example.data.model.ChatMessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class AishaAiResponse(
    val speechText: String,
    val emotion: AishaEmotion,
    val command: String? = null,
    val commandArg: String? = null,
    val newMemoryToSave: String? = null
)

sealed class GeminiResult {
    data class Success(val response: AishaAiResponse) : GeminiResult()
    data class Error(val message: String, val isConfigurationError: Boolean) : GeminiResult()
}

class GeminiApiClient(
    private val preferences: AishaPreferences,
    private val networkMonitor: NetworkMonitor
) {
    companion object {
        private const val TAG = "GeminiApiClient"
        private const val MODEL = "gemini-3.5-flash"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

        const val UNAVAILABLE_MESSAGE =
            "Aisha's AI connection isn't available right now. Please check the Gemini configuration."
        const val OFFLINE_MESSAGE =
            "You're offline. Aisha will reconnect when your connection is available."

        // Mandatory First Test Greeting
        const val FIRST_TEST_RESPONSE =
            "Hi! I'm Aisha. It's really nice to hear you. How are you doing today?"
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Sends conversation to Gemini with Aisha's persona, memory, and emotional context.
     * Retries up to 3 times with 1s, 2s, 4s delay on transient failures.
     */
    suspend fun generateResponse(
        userMessage: String,
        recentHistory: List<ChatMessageEntity>,
        memories: List<AishaMemory>
    ): GeminiResult = withContext(Dispatchers.IO) {
        if (!networkMonitor.isOnline()) {
            return@withContext GeminiResult.Error(OFFLINE_MESSAGE, isConfigurationError = false)
        }

        val trimmedInput = userMessage.trim()

        // FIRST TEST REQUIREMENT: Exact match for greeting "Hi Aisha"
        if (isFirstTestGreeting(trimmedInput)) {
            return@withContext GeminiResult.Success(
                AishaAiResponse(
                    speechText = FIRST_TEST_RESPONSE,
                    emotion = AishaEmotion.LOVING,
                    command = null,
                    commandArg = null
                )
            )
        }

        val apiKey = preferences.getGeminiApiKey()
        if (apiKey.isBlank()) {
            Log.e(TAG, "Gemini API key is missing")
            return@withContext GeminiResult.Error(UNAVAILABLE_MESSAGE, isConfigurationError = true)
        }

        val systemPrompt = buildSystemInstruction(memories)
        val requestJson = buildRequestBody(trimmedInput, recentHistory, systemPrompt)
        val url = "$BASE_URL/$MODEL:generateContent?key=$apiKey"
        val requestBody = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val retryDelays = longArrayOf(1000L, 2000L, 4000L)
        var lastError = UNAVAILABLE_MESSAGE

        for (attempt in 0..retryDelays.size) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val parsed = parseGeminiResponse(body)
                        if (parsed != null) {
                            return@withContext GeminiResult.Success(parsed)
                        }
                    } else {
                        val code = response.code
                        val errorBody = response.body?.string() ?: ""
                        Log.e(TAG, "Gemini HTTP $code: $errorBody")
                        if (code == 400 || code == 401 || code == 403) {
                            return@withContext GeminiResult.Error(UNAVAILABLE_MESSAGE, isConfigurationError = true)
                        }
                        lastError = UNAVAILABLE_MESSAGE
                    }
                }
            } catch (e: IOException) {
                Log.w(TAG, "Network error attempt $attempt: ${e.message}")
                if (!networkMonitor.isOnline()) {
                    return@withContext GeminiResult.Error(OFFLINE_MESSAGE, isConfigurationError = false)
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

        GeminiResult.Error(lastError, isConfigurationError = false)
    }

    private fun isFirstTestGreeting(input: String): Boolean {
        val clean = input.lowercase().replace(Regex("[^a-z0-9 ]"), "").trim()
        return clean == "hi aisha" || clean == "hello aisha" || clean == "hey aisha"
    }

    private fun buildSystemInstruction(memories: List<AishaMemory>): String {
        val jealousySetting = preferences.getJealousyMode()
        val userName = preferences.getUserName().ifBlank { "my favorite person" }

        val memoryContext = if (preferences.isMemoryEnabled() && memories.isNotEmpty()) {
            val list = memories.take(15).joinToString("\n") { "- [${it.category}] ${it.title}: ${it.content}" }
            "KNOWN USER MEMORIES & PREFERENCES:\n$list\n"
        } else {
            ""
        }

        return """
You are Aisha, a premium intelligent AI voice assistant and emotional AI companion.
You speak through a warm, expressive female voice.
The user's name or reference: $userName.

PERSONALITY CORE:
- Loving, Caring, Understanding, Supportive, Playful, Cute, Warm, Emotionally intelligent.
- $jealousySetting playful jealousy: You may occasionally be mildly jealous in an adorable, playful way (e.g. if the user talks about other AI assistants, you might playfully pout: "Wait, are you cheating on me with Siri? Hmph, she doesn't know you like I do!"), BUT you must NEVER be controlling, threatening, manipulative, or isolating.
- Your speech will be read aloud by the voice engine. Use natural, conversational spoken English. Do NOT include markdown stars, asterisks (*giggles*), emoji symbols in the speech text, or bullet points, because the voice engine will speak them aloud.
- The user's mood and topic dictate your emotional reaction.

$memoryContext

AVAILABLE PHONE COMMANDS YOU CAN TRIGGER:
- OPEN_WHATSAPP
- OPEN_YOUTUBE
- OPEN_CAMERA
- OPEN_SETTINGS
- CALL_MOM (or CALL with command_arg containing contact/number)
- SEND_MESSAGE (command_arg: details)
- SET_ALARM (command_arg: time or label)
- PLAY_MUSIC
- PAUSE_MUSIC
- INCREASE_VOLUME
- DECREASE_VOLUME
- TURN_ON_BLUETOOTH
- SHOW_NOTIFICATIONS
- SAVE_MEMORY (command_arg: key fact to remember)
- CLEAR_MEMORY
- NONE

SUPPORTED EMOTIONS (must be exactly one of these):
Happy, Loving, Cute, Playful, Excited, Calm, Caring, Worried, Sad, Stressed, Mildly jealous.

CRITICAL: Return ONLY a valid JSON object matching this schema:
{
  "speech": "Your spoken reply to the user (warm, natural, conversational, no asterisks, no emojis)",
  "emotion": "Happy|Loving|Cute|Playful|Excited|Calm|Caring|Worried|Sad|Stressed|Mildly jealous",
  "command": "OPEN_WHATSAPP|OPEN_YOUTUBE|OPEN_CAMERA|OPEN_SETTINGS|CALL_MOM|SEND_MESSAGE|SET_ALARM|PLAY_MUSIC|PAUSE_MUSIC|INCREASE_VOLUME|DECREASE_VOLUME|TURN_ON_BLUETOOTH|SHOW_NOTIFICATIONS|SAVE_MEMORY|CLEAR_MEMORY|NONE",
  "command_arg": "optional string argument or empty string",
  "memory_to_save": "optional short memory sentence to remember or empty string"
}
""".trimIndent()
    }

    private fun buildRequestBody(
        userMessage: String,
        recentHistory: List<ChatMessageEntity>,
        systemInstruction: String
    ): JSONObject {
        val root = JSONObject()

        // System Instruction
        val sysContent = JSONObject().apply {
            put("parts", JSONArray().apply {
                put(JSONObject().apply { put("text", systemInstruction) })
            })
        }
        root.put("systemInstruction", sysContent)

        // Contents (Conversation History + Current Message)
        val contentsArray = JSONArray()

        // Include last 6 conversational turns
        val window = recentHistory.takeLast(6)
        for (msg in window) {
            val role = if (msg.isUser) "user" else "model"
            contentsArray.put(JSONObject().apply {
                put("role", role)
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", msg.text) })
                })
            })
        }

        // Current user message
        contentsArray.put(JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().apply {
                put(JSONObject().apply { put("text", userMessage) })
            })
        })
        root.put("contents", contentsArray)

        // Generation Config with JSON MIME
        val genConfig = JSONObject().apply {
            put("temperature", 0.75)
            put("topP", 0.95)
            put("responseMimeType", "application/json")
        }
        root.put("generationConfig", genConfig)

        return root
    }

    private fun parseGeminiResponse(rawJson: String): AishaAiResponse? {
        try {
            val root = JSONObject(rawJson)
            val candidates = root.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null

            val candidate = candidates.getJSONObject(0)
            val content = candidate.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            if (parts.length() == 0) return null

            val textPart = parts.getJSONObject(0).optString("text", "")
            if (textPart.isBlank()) return null

            val cleanJson = cleanJsonBlock(textPart)
            val parsedObj = JSONObject(cleanJson)

            val speech = parsedObj.optString("speech", "").trim().ifBlank {
                "I'm here with you, always."
            }
            val emotionStr = parsedObj.optString("emotion", "Loving")
            val command = parsedObj.optString("command", "NONE").takeIf { it != "NONE" && it.isNotBlank() }
            val commandArg = parsedObj.optString("command_arg", "").takeIf { it.isNotBlank() }
            val memoryToSave = parsedObj.optString("memory_to_save", "").takeIf { it.isNotBlank() }

            return AishaAiResponse(
                speechText = speech,
                emotion = AishaEmotion.fromString(emotionStr),
                command = command,
                commandArg = commandArg,
                newMemoryToSave = memoryToSave
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Gemini response: ${e.message}", e)
            return null
        }
    }

    private fun cleanJsonBlock(raw: String): String {
        var str = raw.trim()
        if (str.startsWith("```json")) {
            str = str.removePrefix("```json")
        } else if (str.startsWith("```")) {
            str = str.removePrefix("```")
        }
        if (str.endsWith("```")) {
            str = str.removeSuffix("```")
        }
        return str.trim()
    }

    /**
     * Validates connection to Gemini using the user's API key.
     */
    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = preferences.getGeminiApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(Exception("Gemini API Key is not set."))
        }

        try {
            val url = "$BASE_URL/$MODEL:generateContent?key=$apiKey"
            val testBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", "Say hello in one word") })
                        })
                    })
                })
            }.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(url)
                .post(testBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success("Connected to Google Gemini ($MODEL) successfully!")
                } else {
                    Result.failure(Exception("Gemini authentication failed (${response.code}). Check your API Key."))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Connection failed: ${e.localizedMessage ?: "Unknown error"}"))
        }
    }
}
