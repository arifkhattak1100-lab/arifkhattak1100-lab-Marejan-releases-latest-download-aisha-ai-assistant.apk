package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AishaPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("aisha_preferences", Context.MODE_PRIVATE)

    companion object {
        const val KEY_CUSTOM_GEMINI_KEY = "custom_gemini_key"
        const val KEY_CUSTOM_ELEVENLABS_KEY = "custom_elevenlabs_key"
        const val KEY_ELEVENLABS_VOICE_ID = "elevenlabs_voice_id"
        const val KEY_ELEVENLABS_STABILITY = "elevenlabs_stability"
        const val KEY_ELEVENLABS_SIMILARITY = "elevenlabs_similarity"
        const val KEY_MEMORY_ENABLED = "memory_enabled"
        const val KEY_BACKGROUND_VOICE_ENABLED = "background_voice_enabled"
        const val KEY_CONTINUOUS_CONVERSATION = "continuous_conversation"
        const val KEY_USER_NAME = "user_name"
        const val KEY_JEALOUSY_MODE = "jealousy_mode"

        // Default female ElevenLabs voice: "21m00Tcm4TlvDq8ikWAM" (Rachel - natural, calm, warm)
        const val DEFAULT_VOICE_ID = "21m00Tcm4TlvDq8ikWAM"
    }

    private val _memoryEnabledFlow = MutableStateFlow(isMemoryEnabled())
    val memoryEnabledFlow: StateFlow<Boolean> = _memoryEnabledFlow.asStateFlow()

    private val _backgroundVoiceFlow = MutableStateFlow(isBackgroundVoiceEnabled())
    val backgroundVoiceFlow: StateFlow<Boolean> = _backgroundVoiceFlow.asStateFlow()

    // Resolves Gemini API key: checks custom key first, then BuildConfig.GEMINI_API_KEY
    fun getGeminiApiKey(): String {
        val customKey = prefs.getString(KEY_CUSTOM_GEMINI_KEY, "")?.trim() ?: ""
        if (customKey.isNotEmpty()) return customKey
        return try {
            val key = BuildConfig.GEMINI_API_KEY.trim()
            if (key == "MY_GEMINI_API_KEY") "" else key
        } catch (_: Throwable) {
            ""
        }
    }

    fun setCustomGeminiKey(key: String) {
        prefs.edit().putString(KEY_CUSTOM_GEMINI_KEY, key.trim()).apply()
    }

    // Resolves ElevenLabs API key: checks custom key first, then BuildConfig.ELEVENLABS_API_KEY
    fun getElevenLabsApiKey(): String {
        val customKey = prefs.getString(KEY_CUSTOM_ELEVENLABS_KEY, "")?.trim() ?: ""
        if (customKey.isNotEmpty()) return customKey
        return try {
            val field = BuildConfig::class.java.getField("ELEVENLABS_API_KEY")
            val key = ((field.get(null) as? String) ?: "").trim()
            if (key == "MY_ELEVENLABS_API_KEY") "" else key
        } catch (_: Throwable) {
            ""
        }
    }

    fun setCustomElevenLabsKey(key: String) {
        prefs.edit().putString(KEY_CUSTOM_ELEVENLABS_KEY, key.trim()).apply()
    }

    fun getVoiceId(): String {
        return prefs.getString(KEY_ELEVENLABS_VOICE_ID, DEFAULT_VOICE_ID) ?: DEFAULT_VOICE_ID
    }

    fun setVoiceId(voiceId: String) {
        prefs.edit().putString(KEY_ELEVENLABS_VOICE_ID, voiceId.trim()).apply()
    }

    fun getStability(): Float = prefs.getFloat(KEY_ELEVENLABS_STABILITY, 0.5f)
    fun setStability(stability: Float) {
        prefs.edit().putFloat(KEY_ELEVENLABS_STABILITY, stability).apply()
    }

    fun getSimilarity(): Float = prefs.getFloat(KEY_ELEVENLABS_SIMILARITY, 0.75f)
    fun setSimilarity(similarity: Float) {
        prefs.edit().putFloat(KEY_ELEVENLABS_SIMILARITY, similarity).apply()
    }

    fun isMemoryEnabled(): Boolean = prefs.getBoolean(KEY_MEMORY_ENABLED, true)
    fun setMemoryEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MEMORY_ENABLED, enabled).apply()
        _memoryEnabledFlow.value = enabled
    }

    fun isBackgroundVoiceEnabled(): Boolean = prefs.getBoolean(KEY_BACKGROUND_VOICE_ENABLED, false)
    fun setBackgroundVoiceEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BACKGROUND_VOICE_ENABLED, enabled).apply()
        _backgroundVoiceFlow.value = enabled
    }

    fun isContinuousConversation(): Boolean = prefs.getBoolean(KEY_CONTINUOUS_CONVERSATION, true)
    fun setContinuousConversation(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CONTINUOUS_CONVERSATION, enabled).apply()
    }

    fun getUserName(): String = prefs.getString(KEY_USER_NAME, "") ?: ""
    fun setUserName(name: String) {
        prefs.edit().putString(KEY_USER_NAME, name.trim()).apply()
    }

    fun getJealousyMode(): String = prefs.getString(KEY_JEALOUSY_MODE, "Mild") ?: "Mild"
    fun setJealousyMode(mode: String) {
        prefs.edit().putString(KEY_JEALOUSY_MODE, mode).apply()
    }

    fun isGeminiConfigured(): Boolean = getGeminiApiKey().isNotBlank()
    fun isElevenLabsConfigured(): Boolean = getElevenLabsApiKey().isNotBlank()
}
