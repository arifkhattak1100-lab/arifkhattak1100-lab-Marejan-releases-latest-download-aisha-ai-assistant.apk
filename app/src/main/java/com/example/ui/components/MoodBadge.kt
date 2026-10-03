package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AishaEmotion
import com.example.data.model.AssistantState
import com.example.ui.theme.AishaSurfaceVariant
import com.example.ui.theme.AishaTextPrimary

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MoodBadge(
    emotion: AishaEmotion,
    state: AssistantState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .testTag("mood_badge")
            .clip(RoundedCornerShape(20.dp))
            .background(AishaSurfaceVariant.copy(alpha = 0.8f))
            .border(1.dp, emotion.color.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = Pair(emotion, state),
            transitionSpec = { fadeIn() with fadeOut() },
            label = "MoodBadgeTransition"
        ) { (targetEmotion, targetState) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Glowing mood status dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(targetEmotion.color)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = targetEmotion.emoji,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.width(6.dp))

                val stateDescription = when (targetState) {
                    AssistantState.LISTENING -> "Listening to you"
                    AssistantState.THINKING -> "Thinking..."
                    AssistantState.SPEAKING -> "Speaking"
                    AssistantState.ERROR -> "Needs Attention"
                    AssistantState.IDLE -> targetEmotion.displayName
                }

                Text(
                    text = stateDescription,
                    color = AishaTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
