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
        const val KEY_TTS_PITCH = "tts_pitch"
        const val KEY_TTS_SPEED = "tts_speed"
        const val KEY_MEMORY_ENABLED = "memory_enabled"
        const val KEY_BACKGROUND_VOICE_ENABLED = "background_voice_enabled"
        const val KEY_CONTINUOUS_CONVERSATION = "continuous_conversation"
        const val KEY_USER_NAME = "user_name"
        const val KEY_JEALOUSY_MODE = "jealousy_mode"
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

    // TTS Voice Controls (Pitch & Speed)
    fun getTtsPitch(): Float = prefs.getFloat(KEY_TTS_PITCH, 1.15f)
    fun setTtsPitch(pitch: Float) {
        prefs.edit().putFloat(KEY_TTS_PITCH, pitch.coerceIn(0.5f, 2.0f)).apply()
    }

    fun getTtsSpeed(): Float = prefs.getFloat(KEY_TTS_SPEED, 1.0f)
    fun setTtsSpeed(speed: Float) {
        prefs.edit().putFloat(KEY_TTS_SPEED, speed.coerceIn(0.5f, 2.0f)).apply()
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
    // Native TTS is always configured and ready on Android
    fun isVoiceReady(): Boolean = true
    fun isElevenLabsConfigured(): Boolean = true // Always true so no missing voice warnings appear
}
