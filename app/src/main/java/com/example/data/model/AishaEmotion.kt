package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AishaMoodColors

enum class AishaEmotion(
    val displayName: String,
    val emoji: String,
    val description: String
) {
    HAPPY("Happy", "✨", "Bright, radiant, and cheerful"),
    LOVING("Loving", "💖", "Deeply caring, affectionate, and warm"),
    CUTE("Cute", "🎀", "Adorable, sweet, and endearing"),
    PLAYFUL("Playful", "💫", "Cheeky, teasing, and fun-loving"),
    EXCITED("Excited", "⚡", "Enthusiastic, energetic, and thrilled"),
    CALM("Calm", "🌊", "Peaceful, serene, and grounding"),
    CARING("Caring", "🌸", "Nurturing, attentive, and gentle"),
    WORRIED("Worried", "🍂", "Concerned about you, empathetic"),
    SAD("Sad", "💧", "Soft, sympathetic, and feeling down"),
    STRESSED("Stressed", "🔥", "Overwhelmed or flustered"),
    MILDLY_JEALOUS("Mildly jealous", "💜", "Playfully possessive and pouting");

    val color: Color
        get() = AishaMoodColors.getColorForMood(displayName)

    companion object {
        fun fromString(name: String?): AishaEmotion {
            if (name.isNullOrBlank()) return LOVING
            val clean = name.trim().lowercase()
            return entries.firstOrNull {
                it.displayName.lowercase() == clean ||
                it.name.lowercase() == clean ||
                clean.contains(it.name.lowercase()) ||
                clean.contains(it.displayName.lowercase())
            } ?: LOVING
        }
    }
}

enum class AssistantState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    ERROR
}
