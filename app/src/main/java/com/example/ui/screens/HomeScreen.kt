package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.AssistantState
import com.example.ui.MainViewModel
import com.example.ui.components.AishaOrb
import com.example.ui.components.MoodBadge
import com.example.ui.components.VoiceVisualizer
import com.example.ui.theme.AishaBorder
import com.example.ui.theme.AishaBorderHighlight
import com.example.ui.theme.AishaCyan
import com.example.ui.theme.AishaDarkBackground
import com.example.ui.theme.AishaEmerald
import com.example.ui.theme.AishaMagenta
import com.example.ui.theme.AishaSurface
import com.example.ui.theme.AishaSurfaceGlass
import com.example.ui.theme.AishaSurfaceVariant
import com.example.ui.theme.AishaTextMuted
import com.example.ui.theme.AishaTextPrimary
import com.example.ui.theme.AishaTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToMemory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val assistantState by viewModel.assistantState.collectAsState()
    val emotion by viewModel.currentEmotion.collectAsState()
    val lastSpokenText by viewModel.lastSpokenText.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val actionFeedback by viewModel.actionFeedback.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val speechAmplitude by viewModel.audioPlayer.speechAmplitude.collectAsState()
    val micRms by viewModel.speechManager.rmsNormalized.collectAsState()
    val partialSpeech by viewModel.speechManager.partialText.collectAsState()

    var showTextInputDialog by remember { mutableStateOf(false) }
    var textInputPrompt by remember { mutableStateOf("") }

    // Audio permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startListening()
        }
    }

    val requestMicAndToggle = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.toggleListening()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AishaDarkBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Aisha",
                            color = AishaTextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        // Online indicator dot
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isOnline) AishaEmerald else Color.Red)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToMemory,
                        modifier = Modifier.testTag("nav_memory_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "Memory",
                            tint = AishaCyan
                        )
                    }

                    IconButton(
                        onClick = onNavigateToHistory,
                        modifier = Modifier.testTag("nav_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Conversation History",
                            tint = AishaTextSecondary
                        )
                    }

                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("nav_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = AishaTextSecondary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Mood pill and Error/Action banner
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                MoodBadge(
                    emotion = emotion,
                    state = assistantState
                )

                // Setup Reminder Banner if ElevenLabs or Gemini is not configured
                if (!viewModel.preferences.isElevenLabsConfigured()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clickable { onNavigateToSettings() }
                            .testTag("setup_elevenlabs_banner"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = AishaMagenta.copy(alpha = 0.15f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AishaMagenta.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RecordVoiceOver,
                                    contentDescription = null,
                                    tint = AishaMagenta,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "ElevenLabs API Key Required",
                                        color = AishaTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Tap to configure Aisha's natural voice in Settings",
                                        color = AishaTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Button(
                                onClick = onNavigateToSettings,
                                colors = ButtonDefaults.buttonColors(containerColor = AishaMagenta),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Configure", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Error Notice
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    errorMessage?.let { err ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                                .testTag("error_card"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF3B151E)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF4D4F))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = err,
                                        color = Color(0xFFFFB3BA),
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { viewModel.dismissError() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            tint = Color(0xFFFFB3BA),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                if (err.contains("ElevenLabs", ignoreCase = true) || err.contains("voice", ignoreCase = true)) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = onNavigateToSettings,
                                        colors = ButtonDefaults.buttonColors(containerColor = AishaMagenta),
                                        modifier = Modifier
                                            .align(Alignment.End)
                                            .height(34.dp)
                                            .testTag("fix_elevenlabs_config_button"),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text("Open Voice Settings ⚙️", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else if (err.contains("Gemini", ignoreCase = true) || err.contains("AI connection", ignoreCase = true)) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = onNavigateToSettings,
                                        colors = ButtonDefaults.buttonColors(containerColor = AishaCyan),
                                        modifier = Modifier
                                            .align(Alignment.End)
                                            .height(34.dp)
                                            .testTag("fix_gemini_config_button"),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text("Open AI Settings ⚙️", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Phone Action Feedback Notice
                AnimatedVisibility(
                    visible = actionFeedback != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    actionFeedback?.let { feedback ->
                        Box(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AishaSurfaceVariant.copy(alpha = 0.9f))
                                .border(1.dp, AishaEmerald.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "✓ $feedback",
                                color = AishaEmerald,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Center: The Animated AI Orb
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                val currentAmplitude = when (assistantState) {
                    AssistantState.LISTENING -> micRms
                    AssistantState.SPEAKING -> speechAmplitude
                    else -> 0f
                }

                AishaOrb(
                    state = assistantState,
                    emotion = emotion,
                    audioAmplitude = currentAmplitude,
                    modifier = Modifier.testTag("aisha_orb")
                )
            }

            // Real-time Soundwave Bars
            VoiceVisualizer(
                amplitude = when (assistantState) {
                    AssistantState.LISTENING -> micRms
                    AssistantState.SPEAKING -> speechAmplitude
                    else -> 0f
                },
                color = emotion.color,
                isActive = assistantState == AssistantState.LISTENING || assistantState == AssistantState.SPEAKING
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Speech Transcript Glass Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .widthIn(max = 600.dp)
                    .testTag("speech_transcript_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = AishaSurfaceGlass
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, AishaBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val displayText = if (assistantState == AssistantState.LISTENING && partialSpeech.isNotBlank()) {
                        "You: \"$partialSpeech\""
                    } else {
                        lastSpokenText
                    }

                    Text(
                        text = displayText,
                        color = AishaTextPrimary,
                        fontSize = 16.sp,
                        lineHeight = 23.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Prompt Carousel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val samplePrompts = listOf(
                    "Hi Aisha", // FIRST TEST
                    "How are you feeling?",
                    "Open YouTube",
                    "Open WhatsApp",
                    "Call Mom",
                    "Set an alarm",
                    "Play music",
                    "Increase volume",
                    "Tell me a secret"
                )

                for (prompt in samplePrompts) {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                viewModel.submitTextPrompt(prompt)
                            }
                            .testTag("prompt_chip_${prompt.replace(" ", "_")}"),
                        shape = RoundedCornerShape(16.dp),
                        color = AishaSurfaceVariant.copy(alpha = 0.6f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AishaBorder)
                    ) {
                        Text(
                            text = prompt,
                            color = if (prompt == "Hi Aisha") AishaCyan else AishaTextSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (prompt == "Hi Aisha") FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Action Area: Mic Button, Keyboard Input, Interruption Stop
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Type message button
                    IconButton(
                        onClick = { showTextInputDialog = true },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(AishaSurfaceVariant)
                            .border(1.dp, AishaBorder, CircleShape)
                            .testTag("keyboard_input_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = "Type a message",
                            tint = AishaTextPrimary
                        )
                    }

                    // Main Glowing Microphone Button
                    MicPulseButton(
                        state = assistantState,
                        emotionColor = emotion.color,
                        onClick = requestMicAndToggle
                    )

                    // Interruption / Stop speaking button
                    IconButton(
                        onClick = {
                            if (assistantState == AssistantState.SPEAKING) {
                                viewModel.interruptAisha()
                            } else if (assistantState == AssistantState.LISTENING) {
                                viewModel.stopListening()
                            }
                        },
                        enabled = assistantState == AssistantState.SPEAKING || assistantState == AssistantState.LISTENING,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                if (assistantState == AssistantState.SPEAKING || assistantState == AssistantState.LISTENING)
                                    AishaSurfaceVariant
                                else
                                    AishaSurfaceVariant.copy(alpha = 0.3f)
                            )
                            .border(1.dp, AishaBorder, CircleShape)
                            .testTag("stop_action_button")
                    ) {
                        Icon(
                            imageVector = if (assistantState == AssistantState.SPEAKING) Icons.Default.Stop else Icons.Default.MicOff,
                            contentDescription = "Stop or Interrupt",
                            tint = if (assistantState == AssistantState.SPEAKING) AishaMagenta else AishaTextMuted
                        )
                    }
                }
            }
        }
    }

    // Keyboard Text Input Dialog
    if (showTextInputDialog) {
        AlertDialog(
            onDismissRequest = { showTextInputDialog = false },
            title = {
                Text("Talk to Aisha", color = AishaTextPrimary)
            },
            text = {
                OutlinedTextField(
                    value = textInputPrompt,
                    onValueChange = { textInputPrompt = it },
                    placeholder = { Text("Ask or tell Aisha anything...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("text_input_field"),
                    singleLine = false,
                    maxLines = 4
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val text = textInputPrompt.trim()
                        if (text.isNotBlank()) {
                            viewModel.submitTextPrompt(text)
                            textInputPrompt = ""
                            showTextInputDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AishaCyan),
                    modifier = Modifier.testTag("send_text_button")
                ) {
                    Text("Send", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTextInputDialog = false }) {
                    Text("Cancel", color = AishaTextSecondary)
                }
            },
            containerColor = AishaSurface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun MicPulseButton(
    state: AssistantState,
    emotionColor: Color,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "MicPulseAnimation")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "MicPulseScale"
    )

    val isListening = state == AssistantState.LISTENING

    Box(
        modifier = Modifier.size(88.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing ring when listening
        if (isListening) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(emotionColor.copy(alpha = 0.25f))
            )
        }

        // Main Mic button circle
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = if (isListening) {
                            listOf(emotionColor, AishaMagenta)
                        } else {
                            listOf(AishaCyan, Color(0xFF0072FF))
                        }
                    )
                )
                .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                .clickable { onClick() }
                .testTag("mic_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = if (isListening) "Listening" else "Tap to Speak",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}
