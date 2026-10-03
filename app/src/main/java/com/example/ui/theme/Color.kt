package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Premium Obsidian & Cyber Dark Base
val AishaDarkBackground = Color(0xFF070A10)
val AishaSurface = Color(0xFF0F1422)
val AishaSurfaceVariant = Color(0xFF182034)
val AishaSurfaceGlass = Color(0x331E2942)
val AishaBorder = Color(0x40384D72)
val AishaBorderHighlight = Color(0x8000F2FE)

// Radiant Accent Lights
val AishaCyan = Color(0xFF00F2FE)
val AishaMagenta = Color(0xFFFF2A85)
val AishaPurple = Color(0xFF9030FE)
val AishaViolet = Color(0xFF7928CA)
val AishaEmerald = Color(0xFF00F5A0)
val AishaAmber = Color(0xFFFFB800)
val AishaRose = Color(0xFFFF5376)
val AishaCrimson = Color(0xFFFF3366)

// Text
val AishaTextPrimary = Color(0xFFF1F5F9)
val AishaTextSecondary = Color(0xFF94A3B8)
val AishaTextMuted = Color(0xFF64748B)

// Mood Specific Gradients & Tints
object AishaMoodColors {
    val Happy = Color(0xFF00F2FE)
    val Loving = Color(0xFFFF2A85)
    val Cute = Color(0xFFFF70A6)
    val Playful = Color(0xFFB5179E)
    val Excited = Color(0xFFFF9E00)
    val Calm = Color(0xFF48CAE4)
    val Caring = Color(0xFFFF758F)
    val Worried = Color(0xFFF77F00)
    val Sad = Color(0xFF5390D9)
    val Stressed = Color(0xFFE63946)
    val MildlyJealous = Color(0xFF7209B7)

    fun getColorForMood(mood: String): Color {
        return when (mood.lowercase().trim()) {
            "happy" -> Happy
            "loving" -> Loving
            "cute" -> Cute
            "playful" -> Playful
            "excited" -> Excited
            "calm" -> Calm
            "caring" -> Caring
            "worried" -> Worried
            "sad" -> Sad
            "stressed" -> Stressed
            "mildly jealous", "jealous" -> MildlyJealous
            else -> Loving
        }
    }
}
