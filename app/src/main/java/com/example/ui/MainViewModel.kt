package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AishaApp
import com.example.audio.AishaAudioPlayer
import com.example.audio.SpeechRecognitionManager
import com.example.data.local.AishaPreferences
import com.example.data.model.AishaEmotion
import com.example.data.model.AishaMemory
import com.example.data.model.AssistantState
import com.example.data.model.ChatMessageEntity
import com.example.data.remote.ElevenLabsApiClient
import com.example.data.remote.ElevenLabsResult
import com.example.data.remote.GeminiApiClient
import com.example.data.remote.GeminiResult
import com.example.data.remote.NetworkMonitor
import com.example.phone.PhoneActionManager
import com.example.service.AishaVoiceService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "MainViewModel"
    }

    private val app = application as AishaApp
    private val database = app.database
    val preferences = app.preferences

    val networkMonitor = NetworkMonitor(application)
    val isOnline: StateFlow<Boolean> = networkMonitor.isConnected

    val geminiApiClient = GeminiApiClient(preferences, networkMonitor)
    val elevenLabsApiClient = ElevenLabsApiClient(preferences, networkMonitor)
    val phoneActionManager = PhoneActionManager(application)
    val audioPlayer = AishaAudioPlayer(application)
    val speechManager = SpeechRecognitionManager(application)

    // UI States
    private val _assistantState = MutableStateFlow(AssistantState.IDLE)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _currentEmotion = MutableStateFlow(AishaEmotion.LOVING)
    val currentEmotion: StateFlow<AishaEmotion> = _currentEmotion.asStateFlow()

    private val _lastSpokenText = MutableStateFlow("Hi! Tap the mic or say 'Hi Aisha' to talk to me.")
    val lastSpokenText: StateFlow<String> = _lastSpokenText.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _connectionStatus = MutableStateFlow("Ready")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private val _actionFeedback = MutableStateFlow<String?>(null)
    val actionFeedback: StateFlow<String?> = _actionFeedback.asStateFlow()

    // Persistent Room data
    val messagesFlow: StateFlow<List<ChatMessageEntity>> =
        database.chatMessageDao().getAllMessagesFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memoriesFlow: StateFlow<List<AishaMemory>> =
        database.memoryDao().getAllMemories()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        setupSpeechManager()
        setupAudioPlayerListener()
    }

    private fun setupSpeechManager() {
        speechManager.onSpeechResult = { text ->
            processUserSpeech(text)
        }
        speechManager.onSpeechError = { code, message ->
            Log.w(TAG, "Speech error $code: $message")
            if (_assistantState.value == AssistantState.LISTENING) {
                _assistantState.value = AssistantState.IDLE
            }
        }
    }

    private fun setupAudioPlayerListener() {
        // Player state is monitored directly via audioPlayer.isPlaying
    }

    fun toggleListening() {
        if (_assistantState.value == AssistantState.SPEAKING) {
            // User interrupted Aisha!
            interruptAishaAndListen()
        } else if (_assistantState.value == AssistantState.LISTENING) {
            stopListening()
        } else {
            startListening()
        }
    }

    fun startListening() {
        // Stop audio playback immediately if speaking
        audioPlayer.stop()
        _errorMessage.value = null
        _assistantState.value = AssistantState.LISTENING
        speechManager.startListening()
    }

    fun stopListening() {
        speechManager.stopListening()
        if (_assistantState.value == AssistantState.LISTENING) {
            _assistantState.value = AssistantState.IDLE
        }
    }

    fun interruptAishaAndListen() {
        audioPlayer.stop()
        startListening()
    }

    fun interruptAisha() {
        audioPlayer.stop()
        _assistantState.value = AssistantState.IDLE
    }

    fun processUserSpeech(text: String) {
        if (text.isBlank()) {
            _assistantState.value = AssistantState.IDLE
            return
        }

        viewModelScope.launch {
            // 1. Save User Message to Database
            val userMsg = ChatMessageEntity(
                isUser = true,
                text = text,
                emotion = _currentEmotion.value.displayName
            )
            database.chatMessageDao().insertMessage(userMsg)

            // 2. Set Thinking State
            _assistantState.value = AssistantState.THINKING
            _connectionStatus.value = "Thinking..."

            // 3. Query Gemini AI Brain
            val recent = database.chatMessageDao().getRecentMessages(8).reversed()
            val memories = database.memoryDao().getAllMemoriesList()

            when (val geminiRes = geminiApiClient.generateResponse(text, recent, memories)) {
                is GeminiResult.Error -> {
                    _assistantState.value = AssistantState.ERROR
                    _errorMessage.value = geminiRes.message
                    _lastSpokenText.value = geminiRes.message
                    _connectionStatus.value = "AI Unavailable"
                }

                is GeminiResult.Success -> {
                    val response = geminiRes.response
                    _currentEmotion.value = response.emotion
                    _lastSpokenText.value = response.speechText

                    // Handle Phone Command if triggered
                    var commandResultSummary: String? = null
                    if (!response.command.isNullOrBlank()) {
                        val actionResult = phoneActionManager.executeCommand(
                            response.command,
                            response.commandArg
                        )
                        commandResultSummary = actionResult.feedbackMessage
                        _actionFeedback.value = actionResult.feedbackMessage
                    }

                    // Handle Auto-Memory Saving if requested by Gemini
                    if (!response.newMemoryToSave.isNullOrBlank() && preferences.isMemoryEnabled()) {
                        database.memoryDao().insertMemory(
                            AishaMemory(
                                title = "User Fact",
                                content = response.newMemoryToSave,
                                category = "preference"
                            )
                        )
                    }

                    // Save Aisha's Response to Database
                    val aishaMsg = ChatMessageEntity(
                        isUser = false,
                        text = response.speechText,
                        emotion = response.emotion.displayName,
                        commandExecuted = commandResultSummary
                    )
                    database.chatMessageDao().insertMessage(aishaMsg)

                    // 4. Synthesize Speech via ElevenLabs (Aisha's ONLY voice)
                    _connectionStatus.value = "Generating Voice..."
                    speakWithElevenLabs(response.speechText)
                }
            }
        }
    }

    private suspend fun speakWithElevenLabs(textToSpeak: String) {
        when (val voiceRes = elevenLabsApiClient.textToSpeech(textToSpeak)) {
            is ElevenLabsResult.Error -> {
                _assistantState.value = AssistantState.ERROR
                _errorMessage.value = voiceRes.message
                _connectionStatus.value = "Voice Unavailable"
            }

            is ElevenLabsResult.Success -> {
                _assistantState.value = AssistantState.SPEAKING
                _connectionStatus.value = "Speaking"
                withContext(Dispatchers.Main) {
                    audioPlayer.playAudio(voiceRes.audioData) {
                        // Playback completed
                        if (preferences.isContinuousConversation()) {
                            // Smooth continuous voice conversation
                            startListening()
                        } else {
                            _assistantState.value = AssistantState.IDLE
                            _connectionStatus.value = "Ready"
                        }
                    }
                }
            }
        }
    }

    fun submitTextPrompt(prompt: String) {
        processUserSpeech(prompt)
    }

    // Memory Management
    fun addMemory(title: String, content: String, category: String) {
        viewModelScope.launch {
            database.memoryDao().insertMemory(
                AishaMemory(
                    title = title.trim(),
                    content = content.trim(),
                    category = category.trim()
                )
            )
        }
    }

    fun deleteMemory(memory: AishaMemory) {
        viewModelScope.launch {
            database.memoryDao().deleteMemory(memory)
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            database.memoryDao().clearAllMemories()
        }
    }

    fun setMemoryEnabled(enabled: Boolean) {
        preferences.setMemoryEnabled(enabled)
    }

    // Background Voice Management
    fun setBackgroundVoiceEnabled(enabled: Boolean) {
        preferences.setBackgroundVoiceEnabled(enabled)
        if (enabled) {
            AishaVoiceService.start(getApplication())
        } else {
            AishaVoiceService.stop(getApplication())
        }
    }

    // History
    fun clearChatHistory() {
        viewModelScope.launch {
            database.chatMessageDao().clearHistory()
            _lastSpokenText.value = "Chat history cleared. What would you like to talk about?"
        }
    }

    fun dismissError() {
        _errorMessage.value = null
        if (_assistantState.value == AssistantState.ERROR) {
            _assistantState.value = AssistantState.IDLE
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
        speechManager.destroy()
    }
}
