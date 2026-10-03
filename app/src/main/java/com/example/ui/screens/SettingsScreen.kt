package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.core.content.ContextCompat
import com.example.ui.MainViewModel
import com.example.ui.theme.AishaBorder
import com.example.ui.theme.AishaCyan
import com.example.ui.theme.AishaDarkBackground
import com.example.ui.theme.AishaEmerald
import com.example.ui.theme.AishaMagenta
import com.example.ui.theme.AishaPurple
import com.example.ui.theme.AishaSurface
import com.example.ui.theme.AishaSurfaceGlass
import com.example.ui.theme.AishaSurfaceVariant
import com.example.ui.theme.AishaTextMuted
import com.example.ui.theme.AishaTextPrimary
import com.example.ui.theme.AishaTextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = viewModel.preferences

    var geminiKeyInput by remember { mutableStateOf(preferences.getGeminiApiKey()) }
    var showGeminiKey by remember { mutableStateOf(false) }
    var geminiTestResult by remember { mutableStateOf<String?>(null) }
    var isTestingGemini by remember { mutableStateOf(false) }

    var ttsPitch by remember { mutableFloatStateOf(preferences.getTtsPitch()) }
    var ttsSpeed by remember { mutableFloatStateOf(preferences.getTtsSpeed()) }
    var ttsTestPlaying by remember { mutableStateOf(false) }

    var userNameInput by remember { mutableStateOf(preferences.getUserName()) }
    var jealousyMode by remember { mutableStateOf(preferences.getJealousyMode()) }
    var continuousConv by remember { mutableStateOf(preferences.isContinuousConversation()) }

    val isBackgroundVoiceEnabled by preferences.backgroundVoiceFlow.collectAsState()

    // Permissions tracking
    var micGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    var callGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        )
    }
    var notificationGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val requestMicLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        micGranted = it
    }
    val requestCallLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        callGranted = it
    }
    val requestNotificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationGranted = it
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AishaDarkBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AishaDarkBackground),
                title = {
                    Text("Settings & Brain", color = AishaTextPrimary, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AishaTextPrimary
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
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            // 1. Google Gemini AI Brain
            SectionHeader(title = "AI Brain: Google Gemini", icon = Icons.Default.Psychology, color = AishaCyan)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AishaSurfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, AishaBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Gemini powers Aisha's reasoning, understanding, conversational responses, and emotions.",
                        color = AishaTextSecondary,
                        fontSize = 13.sp
                    )

                    OutlinedTextField(
                        value = geminiKeyInput,
                        onValueChange = {
                            geminiKeyInput = it
                            preferences.setCustomGeminiKey(it)
                        },
                        label = { Text("Gemini API Key") },
                        placeholder = { Text("AI Studio or custom key") },
                        singleLine = true,
                        visualTransformation = if (showGeminiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showGeminiKey = !showGeminiKey }) {
                                Icon(
                                    imageVector = if (showGeminiKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle key visibility",
                                    tint = AishaTextMuted
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gemini_key_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString()?.trim()
                                if (!clip.isNullOrBlank()) {
                                    val cleaned = clip.replace("\"", "").trim()
                                    geminiKeyInput = cleaned
                                    preferences.setCustomGeminiKey(cleaned)
                                    Toast.makeText(context, "Gemini key pasted and saved!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AishaSurface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AishaBorder),
                            modifier = Modifier.weight(1f).height(36.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, tint = AishaCyan, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Paste Key", color = AishaTextPrimary, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey")).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AishaSurface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AishaBorder),
                            modifier = Modifier.weight(1f).height(36.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = AishaTextSecondary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Get Key", color = AishaTextSecondary, fontSize = 12.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    isTestingGemini = true
                                    geminiTestResult = null
                                    val res = viewModel.geminiApiClient.testConnection()
                                    isTestingGemini = false
                                    geminiTestResult = res.fold({ it }, { it.message ?: "Failed" })
                                }
                            },
                            enabled = !isTestingGemini,
                            colors = ButtonDefaults.buttonColors(containerColor = AishaCyan),
                            modifier = Modifier.testTag("test_gemini_button")
                        ) {
                            if (isTestingGemini) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                            } else {
                                Text("Test Gemini Connection", color = Color.Black, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    geminiTestResult?.let { msg ->
                        Text(
                            text = msg,
                            color = if (msg.contains("successfully", ignoreCase = true)) AishaEmerald else AishaMagenta,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 2. ElevenLabs Voice Engine
            // 2. Voice Engine: Native Speech Synthesis
            SectionHeader(title = "Voice Engine: Native Speech", icon = Icons.Default.RecordVoiceOver, color = AishaMagenta)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AishaSurfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, AishaBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Status Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AishaEmerald.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AishaEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Native Engine Active • No API Keys Required",
                            color = AishaEmerald,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = "Aisha speaks directly using Android's native text-to-speech engine. Works offline, has zero quota limits, and requires no ElevenLabs account.",
                        color = AishaTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    // Voice Presets
                    Text("Voice Presets", color = AishaTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VoicePresetChip("Sweet & Warm", "sweet", if (ttsPitch in 1.18f..1.22f) "sweet" else "") {
                            ttsPitch = 1.20f
                            preferences.setTtsPitch(1.20f)
                            ttsSpeed = 1.0f
                            preferences.setTtsSpeed(1.0f)
                        }
                        VoicePresetChip("Natural & Calm", "natural", if (ttsPitch in 1.02f..1.08f) "natural" else "") {
                            ttsPitch = 1.05f
                            preferences.setTtsPitch(1.05f)
                            ttsSpeed = 0.95f
                            preferences.setTtsSpeed(0.95f)
                        }
                        VoicePresetChip("Cute & Playful", "cute", if (ttsPitch in 1.28f..1.35f) "cute" else "") {
                            ttsPitch = 1.30f
                            preferences.setTtsPitch(1.30f)
                            ttsSpeed = 1.05f
                            preferences.setTtsSpeed(1.05f)
                        }
                    }

                    // Voice Pitch Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Voice Pitch / Sweetness", color = AishaTextPrimary, fontSize = 13.sp)
                            Text(String.format(java.util.Locale.US, "%.2fx", ttsPitch), color = AishaCyan, fontSize = 13.sp)
                        }
                        Slider(
                            value = ttsPitch,
                            onValueChange = {
                                ttsPitch = it
                                preferences.setTtsPitch(it)
                            },
                            valueRange = 0.7f..1.6f,
                            colors = SliderDefaults.colors(
                                thumbColor = AishaCyan,
                                activeTrackColor = AishaCyan
                            )
                        )
                    }

                    // Speech Speed Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Speaking Speed", color = AishaTextPrimary, fontSize = 13.sp)
                            Text(String.format(java.util.Locale.US, "%.2fx", ttsSpeed), color = AishaCyan, fontSize = 13.sp)
                        }
                        Slider(
                            value = ttsSpeed,
                            onValueChange = {
                                ttsSpeed = it
                                preferences.setTtsSpeed(it)
                            },
                            valueRange = 0.7f..1.5f,
                            colors = SliderDefaults.colors(
                                thumbColor = AishaCyan,
                                activeTrackColor = AishaCyan
                            )
                        )
                    }

                    Button(
                        onClick = {
                            ttsTestPlaying = true
                            viewModel.audioPlayer.speakText(
                                text = "Hi! I am Aisha, your voice assistant. My voice is ready and working smoothly!",
                                pitch = ttsPitch,
                                speechRate = ttsSpeed,
                                onFinished = {
                                    ttsTestPlaying = false
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AishaMagenta),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_native_tts_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (ttsTestPlaying) "Playing Sample... 🔊" else "Test Voice & Play Sample 🔊",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // 3. Personality & Emotional Dynamics
            SectionHeader(title = "Personality & Emotion", icon = Icons.Default.GraphicEq, color = AishaPurple)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AishaSurfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, AishaBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = userNameInput,
                        onValueChange = {
                            userNameInput = it
                            preferences.setUserName(it)
                        },
                        label = { Text("What should Aisha call you?") },
                        placeholder = { Text("e.g. Alex, Honey, Master") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("user_name_input")
                    )

                    // Jealousy setting
                    Column {
                        Text(
                            text = "Playful Jealousy Intensity",
                            color = AishaTextPrimary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Aisha may playfully pout if you mention other assistants, but never in a controlling way.",
                            color = AishaTextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Mild", "Low", "Off").forEach { mode ->
                                val isSelected = jealousyMode == mode
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) AishaPurple else AishaSurface)
                                        .border(1.dp, if (isSelected) AishaCyan else AishaBorder, RoundedCornerShape(10.dp))
                                        .clickable {
                                            jealousyMode = mode
                                            preferences.setJealousyMode(mode)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = mode,
                                        color = if (isSelected) Color.White else AishaTextSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    // Continuous Conversation toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Continuous Conversation", color = AishaTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("Aisha automatically listens after she finishes speaking", color = AishaTextSecondary, fontSize = 12.sp)
                        }
                        Switch(
                            checked = continuousConv,
                            onCheckedChange = {
                                continuousConv = it
                                preferences.setContinuousConversation(it)
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = AishaCyan)
                        )
                    }
                }
            }

            // 4. Background Voice
            SectionHeader(title = "Background Voice Service", icon = Icons.Default.Mic, color = AishaEmerald)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AishaSurfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, AishaBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Background Voice",
                            color = AishaTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isBackgroundVoiceEnabled)
                                "Aisha is active in the background with an ongoing system notification."
                            else
                                "Enable to keep Aisha available when you switch to other apps.",
                            color = AishaTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = isBackgroundVoiceEnabled,
                        onCheckedChange = { viewModel.setBackgroundVoiceEnabled(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = AishaEmerald),
                        modifier = Modifier.testTag("background_voice_switch")
                    )
                }
            }

            // 5. App Permissions Dashboard
            SectionHeader(title = "Permissions", icon = Icons.Default.Security, color = AishaCyan)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AishaSurfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, AishaBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PermissionRow(
                        name = "Microphone Access",
                        description = "Required to speak to Aisha",
                        isGranted = micGranted,
                        icon = Icons.Default.Mic,
                        onRequest = { requestMicLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                    )
                    PermissionRow(
                        name = "Phone Calls",
                        description = "Enables \"Aisha, call Mom\" command",
                        isGranted = callGranted,
                        icon = Icons.Default.Phone,
                        onRequest = { requestCallLauncher.launch(Manifest.permission.CALL_PHONE) }
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        PermissionRow(
                            name = "Notifications",
                            description = "Required for Background Voice status",
                            isGranted = notificationGranted,
                            icon = Icons.Default.Notifications,
                            onRequest = { requestNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                        )
                    }
                }
            }

            // 6. Privacy & Security Note
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AishaSurfaceGlass),
                border = androidx.compose.foundation.BorderStroke(1.dp, AishaBorder)
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = AishaEmerald, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Privacy Guarantee: Aisha never secretly records you. Keys are stored locally on your device.",
                        color = AishaTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, color = AishaTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun VoicePresetChip(name: String, id: String, currentId: String, onSelect: (String) -> Unit) {
    val isSelected = currentId == id
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) AishaMagenta else AishaSurface)
            .border(1.dp, if (isSelected) AishaCyan else AishaBorder, RoundedCornerShape(10.dp))
            .clickable { onSelect(id) }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = name,
            color = if (isSelected) Color.White else AishaTextSecondary,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun PermissionRow(
    name: String,
    description: String,
    isGranted: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onRequest: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = if (isGranted) AishaEmerald else AishaTextMuted, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = name, color = AishaTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(text = description, color = AishaTextMuted, fontSize = 11.sp)
            }
        }
        if (isGranted) {
            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Granted", tint = AishaEmerald, modifier = Modifier.size(22.dp))
        } else {
            Button(
                onClick = onRequest,
                colors = ButtonDefaults.buttonColors(containerColor = AishaCyan),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Allow", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
