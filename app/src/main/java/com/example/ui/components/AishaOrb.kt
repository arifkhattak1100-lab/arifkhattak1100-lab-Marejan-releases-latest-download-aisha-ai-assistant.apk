package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.data.model.AishaEmotion
import com.example.data.model.AssistantState
import com.example.ui.theme.AishaCyan
import com.example.ui.theme.AishaMagenta
import com.example.ui.theme.AishaPurple
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AishaOrb(
    state: AssistantState,
    emotion: AishaEmotion,
    audioAmplitude: Float, // 0f to 1f (from microphone RMS or voice speech)
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "AishaOrbAnimations")

    // Gentle breathing pulse for IDLE
    val idlePulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "IdlePulse"
    )

    // Continuous rotation for aura rings
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbRotation"
    )

    // Fast rotation for THINKING state
    val thinkingSpin by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ThinkingSpin"
    )

    // Wave ripple progression
    val rippleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RippleProgress"
    )

    val primaryMoodColor = emotion.color
    val secondaryMoodColor = when (emotion) {
        AishaEmotion.LOVING -> AishaMagenta
        AishaEmotion.HAPPY -> AishaCyan
        AishaEmotion.CUTE -> Color(0xFFFF70A6)
        AishaEmotion.PLAYFUL -> AishaPurple
        AishaEmotion.EXCITED -> Color(0xFFFF9E00)
        AishaEmotion.CALM -> Color(0xFF48CAE4)
        AishaEmotion.CARING -> Color(0xFFFF758F)
        AishaEmotion.WORRIED -> Color(0xFFF77F00)
        AishaEmotion.SAD -> Color(0xFF5390D9)
        AishaEmotion.STRESSED -> Color(0xFFE63946)
        AishaEmotion.MILDLY_JEALOUS -> Color(0xFF9030FE)
    }

    Box(
        modifier = modifier.size(240.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(240.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.minDimension * 0.32f

            // Scale computation depending on state
            val dynamicScale = when (state) {
                AssistantState.IDLE -> idlePulse
                AssistantState.LISTENING -> (1f + audioAmplitude * 0.35f).coerceIn(1f, 1.45f)
                AssistantState.THINKING -> (1f + sin(Math.toRadians(thinkingSpin.toDouble())).toFloat() * 0.08f)
                AssistantState.SPEAKING -> (1f + audioAmplitude * 0.25f).coerceIn(1f, 1.35f)
                AssistantState.ERROR -> 0.95f
            }

            val currentRadius = baseRadius * dynamicScale

            // Draw outer ambient glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryMoodColor.copy(alpha = 0.45f),
                        secondaryMoodColor.copy(alpha = 0.2f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = currentRadius * 1.8f
                ),
                radius = currentRadius * 1.8f,
                center = center
            )

            // Draw Listening / Speaking expansion shockwaves
            if (state == AssistantState.LISTENING || state == AssistantState.SPEAKING) {
                val waveRadius = currentRadius + (rippleProgress * currentRadius * 0.8f)
                val waveAlpha = (1f - rippleProgress) * 0.6f
                drawCircle(
                    color = primaryMoodColor.copy(alpha = waveAlpha),
                    radius = waveRadius,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )

                val waveRadius2 = currentRadius + (((rippleProgress + 0.5f) % 1f) * currentRadius * 0.8f)
                val waveAlpha2 = (1f - ((rippleProgress + 0.5f) % 1f)) * 0.4f
                drawCircle(
                    color = secondaryMoodColor.copy(alpha = waveAlpha2),
                    radius = waveRadius2,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // Draw Thinking particle orbits
            if (state == AssistantState.THINKING) {
                drawThinkingOrbit(center, currentRadius * 1.3f, thinkingSpin, primaryMoodColor)
                drawThinkingOrbit(center, currentRadius * 1.5f, -thinkingSpin * 1.4f, secondaryMoodColor)
            }

            // Outer orbital ring
            val angleRad = Math.toRadians(rotationAngle.toDouble())
            val orbitalOffset = Offset(
                center.x + (cos(angleRad) * currentRadius * 0.15f).toFloat(),
                center.y + (sin(angleRad) * currentRadius * 0.15f).toFloat()
            )

            // Core Sphere Gradient
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        primaryMoodColor.copy(alpha = 0.9f),
                        secondaryMoodColor.copy(alpha = 0.85f),
                        Color(0xFF0F1426)
                    ),
                    center = Offset(
                        center.x - currentRadius * 0.25f,
                        center.y - currentRadius * 0.25f
                    ),
                    radius = currentRadius * 1.1f
                ),
                radius = currentRadius,
                center = center
            )

            // Dynamic Inner Specular Highlight
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.7f),
                        Color.Transparent
                    ),
                    center = Offset(center.x - currentRadius * 0.35f, center.y - currentRadius * 0.35f),
                    radius = currentRadius * 0.5f
                ),
                radius = currentRadius * 0.5f,
                center = Offset(center.x - currentRadius * 0.35f, center.y - currentRadius * 0.35f)
            )

            // Atmospheric edge ring
            drawCircle(
                color = primaryMoodColor.copy(alpha = 0.6f),
                radius = currentRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

private fun DrawScope.drawThinkingOrbit(
    center: Offset,
    orbitRadius: Float,
    angleDeg: Float,
    color: Color
) {
    val rad = Math.toRadians(angleDeg.toDouble())
    val dotCenter = Offset(
        (center.x + cos(rad) * orbitRadius).toFloat(),
        (center.y + sin(rad) * orbitRadius).toFloat()
    )
    drawCircle(
        color = color.copy(alpha = 0.8f),
        radius = 5.dp.toPx(),
        center = dotCenter
    )
    drawCircle(
        color = color.copy(alpha = 0.3f),
        radius = 10.dp.toPx(),
        center = dotCenter
    )
}
