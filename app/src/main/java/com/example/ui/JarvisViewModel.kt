package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.ConnectionTestResult
import com.example.ai.GeminiConfig
import com.example.ai.GeminiContent
import com.example.ai.GeminiLiveService
import com.example.ai.GeminiPart
import com.example.ai.GeminiRestService
import com.example.ai.RestResult
import com.example.automation.JarvisAccessibilityService
import com.example.automation.JarvisTools
import com.example.automation.PhoneAutomationManager
import com.example.data.local.ChatMessageEntity
import com.example.data.local.JarvisDatabase
import com.example.data.local.JarvisRepository
import com.example.data.local.MemoryEntity
import com.example.data.local.NoteEntity
import com.example.ui.components.AssistantVisualState
import com.example.voice.AudioRecordManager
import com.example.voice.AudioTrackPlayer
import com.example.voice.JarvisSpeechRecognizer
import com.example.voice.TtsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class JarvisTab {
    HUD,
    CALLING,
    TERMINAL,
    MEMORY_NOTES,
    SETTINGS
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val database = JarvisDatabase.getInstance(context)
    val repository = JarvisRepository(database)
    val automationManager = PhoneAutomationManager(context)
    val tools = JarvisTools(context, repository, automationManager)
    val restService = GeminiRestService(tools)

    // UI Navigation State
    private val _currentTab = MutableStateFlow(JarvisTab.HUD)
    val currentTab: StateFlow<JarvisTab> = _currentTab.asStateFlow()

    // Assistant Status
    private val _assistantState = MutableStateFlow(AssistantVisualState.STANDBY)
    val assistantState: StateFlow<AssistantVisualState> = _assistantState.asStateFlow()

    private val _statusMessage = MutableStateFlow("JARVIS Systems Online")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    private val _callingModeActive = MutableStateFlow(false)
    val callingModeActive: StateFlow<Boolean> = _callingModeActive.asStateFlow()

    private val _isMicMuted = MutableStateFlow(false)
    val isMicMuted: StateFlow<Boolean> = _isMicMuted.asStateFlow()

    private val _liveConnectionStatus = MutableStateFlow("Standby")
    val liveConnectionStatus: StateFlow<String> = _liveConnectionStatus.asStateFlow()

    // Settings States
    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _liveModel = MutableStateFlow(GeminiConfig.DEFAULT_LIVE_MODEL)
    val liveModel: StateFlow<String> = _liveModel.asStateFlow()

    private val _textModel = MutableStateFlow(GeminiConfig.DEFAULT_TEXT_MODEL)
    val textModel: StateFlow<String> = _textModel.asStateFlow()

    private val _voiceName = MutableStateFlow("Puck")
    val voiceName: StateFlow<String> = _voiceName.asStateFlow()

    private val _wakeWordEnabled = MutableStateFlow(true)
    val wakeWordEnabled: StateFlow<Boolean> = _wakeWordEnabled.asStateFlow()

    private val _wakeWordPhrase = MutableStateFlow("JARVIS")
    val wakeWordPhrase: StateFlow<String> = _wakeWordPhrase.asStateFlow()

    private val _wakeWordSensitivity = MutableStateFlow(0.7f)
    val wakeWordSensitivity: StateFlow<Float> = _wakeWordSensitivity.asStateFlow()

    private val _personality = MutableStateFlow(GeminiConfig.PersonalityMode.NORMAL)
    val personality: StateFlow<GeminiConfig.PersonalityMode> = _personality.asStateFlow()

    private val _memoryEnabled = MutableStateFlow(true)
    val memoryEnabled: StateFlow<Boolean> = _memoryEnabled.asStateFlow()

    private val _continuousCallingEnabled = MutableStateFlow(true)
    val continuousCallingEnabled: StateFlow<Boolean> = _continuousCallingEnabled.asStateFlow()

    private val _bargeInEnabled = MutableStateFlow(true)
    val bargeInEnabled: StateFlow<Boolean> = _bargeInEnabled.asStateFlow()

    private val _speechSpeed = MutableStateFlow(1.0f)
    val speechSpeed: StateFlow<Float> = _speechSpeed.asStateFlow()

    // Test API Key Feedback
    private val _testResult = MutableStateFlow<ConnectionTestResult?>(null)
    val testResult: StateFlow<ConnectionTestResult?> = _testResult.asStateFlow()

    private val _isTestingKey = MutableStateFlow(false)
    val isTestingKey: StateFlow<Boolean> = _isTestingKey.asStateFlow()

    // Accessibility Service Status
    private val _isAccessibilityEnabled = MutableStateFlow(false)
    val isAccessibilityEnabled: StateFlow<Boolean> = _isAccessibilityEnabled.asStateFlow()

    // Room Database Flows
    private val _notesSearchQuery = MutableStateFlow("")
    val notesSearchQuery: StateFlow<String> = _notesSearchQuery.asStateFlow()

    val allNotes: StateFlow<List<NoteEntity>> = _notesSearchQuery.flatMapLatest { q ->
        if (q.isBlank()) repository.allNotes else repository.searchNotes(q)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMemories: StateFlow<List<MemoryEntity>> = repository.allMemories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatMessages: StateFlow<List<ChatMessageEntity>> = repository.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Voice & Audio Subsystems
    private val audioTrackPlayer: AudioTrackPlayer = AudioTrackPlayer(
        onPlaybackStarted = {
            _assistantState.value = AssistantVisualState.SPEAKING
            _statusMessage.value = "JARVIS is speaking..."
            audioRecordManager.isJarvisSpeaking = true
        },
        onPlaybackEnded = {
            audioRecordManager.isJarvisSpeaking = false
            onAssistantFinishedSpeaking()
        }
    )

    private val ttsManager: TtsManager = TtsManager(
        context = context,
        onStart = {
            _assistantState.value = AssistantVisualState.SPEAKING
            _statusMessage.value = "JARVIS is speaking..."
            audioRecordManager.isJarvisSpeaking = true
        },
        onDone = {
            audioRecordManager.isJarvisSpeaking = false
            onAssistantFinishedSpeaking()
        }
    )

    private val audioRecordManager: AudioRecordManager = AudioRecordManager(
        onAudioChunk = { chunk ->
            if (!_isMicMuted.value && this::liveService.isInitialized && liveService.isConnected()) {
                liveService.sendAudioChunk(chunk)
            }
        },
        onRmsLevel = { rms ->
            _audioLevel.value = rms
        },
        onSpeechEnded = { _ ->
            // End-of-speech detected from mic audio
            if (_assistantState.value == AssistantVisualState.LISTENING) {
                // Speech finished; transition to thinking
                _assistantState.value = AssistantVisualState.THINKING
                _statusMessage.value = "Processing command..."
            }
        },
        onBargeIn = {
            if (_bargeInEnabled.value) {
                handleBargeIn()
            }
        }
    )

    private val speechRecognizer: JarvisSpeechRecognizer = JarvisSpeechRecognizer(
        context = context,
        onSpeechResult = { spokenText ->
            handleSpokenCommand(spokenText)
        },
        onSpeechPartialResult = { partial ->
            _liveTranscript.value = partial
        },
        onSpeechRmsLevel = { level ->
            if (_assistantState.value == AssistantVisualState.LISTENING) {
                _audioLevel.value = level
            }
        },
        onSpeechStopCommand = {
            handleStopCommand()
        },
        onSpeechWakeWordDetected = {
            handleWakeWord()
        },
        onSpeechError = { _ ->
            // Handled automatically in recognizer loop
        }
    )

    private lateinit var liveService: GeminiLiveService

    private var thinkJob: Job? = null

    init {
        liveService = GeminiLiveService(
            scope = viewModelScope,
            onConnected = {
                _liveConnectionStatus.value = "Live Connected"
                _statusMessage.value = "Gemini Live Active"
            },
            onDisconnected = { _ ->
                _liveConnectionStatus.value = "Disconnected"
            },
            onAudioChunkReceived = { pcmBytes ->
                audioTrackPlayer.playChunk(pcmBytes)
            },
            onTranscriptReceived = { text, isUser ->
                _liveTranscript.value = text
                if (!isUser) {
                    viewModelScope.launch {
                        repository.addMessage(role = "assistant", content = text)
                    }
                }
            },
            onInterrupted = {
                handleBargeIn()
            },
            onTurnComplete = {
                // Live turn complete; handled in audio player end
            },
            onToolCallReceived = { callId, name, args ->
                viewModelScope.launch {
                    _assistantState.value = AssistantVisualState.THINKING
                    _statusMessage.value = "Executing: $name..."
                    val res = tools.executeTool(name, args)
                    repository.addMessage(role = "system", content = "Tool executed: $name -> ${res.output}")
                    liveService.sendToolResponse(callId, name, res.output)
                }
            },
            onError = { err ->
                _statusMessage.value = err
            }
        )
        audioTrackPlayer.initialize(viewModelScope)
        loadStoredSettings()
        checkAccessibilityStatus()
    }

    private fun loadStoredSettings() {
        viewModelScope.launch {
            val storedKey = repository.getSetting("gemini_api_key", "")
            _apiKey.value = storedKey

            val storedLiveModel = repository.getSetting("live_model", GeminiConfig.DEFAULT_LIVE_MODEL)
            _liveModel.value = storedLiveModel

            val storedTextModel = repository.getSetting("text_model", GeminiConfig.DEFAULT_TEXT_MODEL)
            _textModel.value = storedTextModel

            val storedVoice = repository.getSetting("live_voice", "Puck")
            _voiceName.value = storedVoice

            val storedWake = repository.getSetting("wake_word_enabled", "true").toBoolean()
            _wakeWordEnabled.value = storedWake

            val storedPhrase = repository.getSetting("wake_word_phrase", "JARVIS")
            _wakeWordPhrase.value = storedPhrase
            speechRecognizer.setWakeWordConfig(storedWake, storedPhrase)

            val storedPersonality = repository.getSetting("personality_mode", "NORMAL")
            _personality.value = try {
                GeminiConfig.PersonalityMode.valueOf(storedPersonality)
            } catch (e: Exception) {
                GeminiConfig.PersonalityMode.NORMAL
            }

            val storedMemory = repository.getSetting("memory_enabled", "true").toBoolean()
            _memoryEnabled.value = storedMemory

            val storedCalling = repository.getSetting("continuous_calling", "true").toBoolean()
            _continuousCallingEnabled.value = storedCalling

            val storedBargeIn = repository.getSetting("barge_in_enabled", "true").toBoolean()
            _bargeInEnabled.value = storedBargeIn

            val storedSpeed = repository.getSetting("speech_speed", "1.0").toFloatOrNull() ?: 1.0f
            _speechSpeed.value = storedSpeed
            ttsManager.setSpeechRate(storedSpeed)
        }
    }

    fun setTab(tab: JarvisTab) {
        _currentTab.value = tab
        if (tab == JarvisTab.CALLING) {
            startCallingMode()
        } else if (_callingModeActive.value && tab != JarvisTab.CALLING) {
            // Keep background calling mode or exit if intended
        }
    }

    fun checkAccessibilityStatus() {
        _isAccessibilityEnabled.value = JarvisAccessibilityService.isEnabled(context)
    }

    fun openAccessibilitySettings() {
        JarvisAccessibilityService.openAccessibilitySettings(context)
    }

    // -------------------------------------------------------------
    // Calling Mode & Continuous Loop (Listen -> Think -> Speak -> Listen)
    // -------------------------------------------------------------

    fun startCallingMode() {
        _callingModeActive.value = true
        _assistantState.value = AssistantVisualState.LISTENING
        _statusMessage.value = "Continuous Voice Active. Speak anytime."

        val effectiveKey = GeminiConfig.getEffectiveApiKey(_apiKey.value)
        if (effectiveKey.isNotBlank()) {
            _liveConnectionStatus.value = "Connecting..."
            viewModelScope.launch {
                val mem = if (_memoryEnabled.value) repository.getFormattedMemoryContext() else ""
                val sysInst = GeminiConfig.getSystemInstruction(_personality.value, mem)
                liveService.connect(
                    apiKey = effectiveKey,
                    modelName = _liveModel.value,
                    voiceName = _voiceName.value,
                    systemInstruction = sysInst
                )
            }
        }

        audioRecordManager.startRecording(viewModelScope)
        speechRecognizer.startListening(continuous = true)
    }

    fun endCallingMode() {
        _callingModeActive.value = false
        _assistantState.value = AssistantVisualState.STANDBY
        _statusMessage.value = "Calling Mode Terminated"
        liveService.disconnect("User ended call")
        audioRecordManager.stopRecording()
        speechRecognizer.stopListening()
        audioTrackPlayer.interrupt()
        ttsManager.stop()
        _liveConnectionStatus.value = "Standby"
        _currentTab.value = JarvisTab.HUD
    }

    fun toggleMicMute() {
        _isMicMuted.value = !_isMicMuted.value
        _statusMessage.value = if (_isMicMuted.value) "Microphone Muted" else "Microphone Active"
    }

    fun toggleCallingMode() {
        if (_callingModeActive.value) {
            endCallingMode()
        } else {
            setTab(JarvisTab.CALLING)
        }
    }

    // -------------------------------------------------------------
    // Wake Word & Voice Processing
    // -------------------------------------------------------------

    private fun handleWakeWord() {
        _assistantState.value = AssistantVisualState.LISTENING
        _statusMessage.value = "Yes, I'm listening."
        _liveTranscript.value = ""
        ttsManager.speak("Yes, I'm listening.")
    }

    fun handleStopCommand() {
        _assistantState.value = AssistantVisualState.INTERRUPTED
        _statusMessage.value = "Action halted."
        audioTrackPlayer.interrupt()
        ttsManager.stop()
        thinkJob?.cancel()

        viewModelScope.launch {
            delay(800)
            if (_callingModeActive.value) {
                _assistantState.value = AssistantVisualState.LISTENING
                _statusMessage.value = "Listening..."
            } else {
                _assistantState.value = AssistantVisualState.STANDBY
                _statusMessage.value = "JARVIS Standby"
            }
        }
    }

    private fun handleBargeIn() {
        _assistantState.value = AssistantVisualState.INTERRUPTED
        _statusMessage.value = "Interrupted by user..."
        audioTrackPlayer.interrupt()
        ttsManager.stop()
        thinkJob?.cancel()

        viewModelScope.launch {
            delay(350)
            _assistantState.value = AssistantVisualState.LISTENING
            _statusMessage.value = "Listening to your command..."
        }
    }

    fun handleSpokenCommand(command: String) {
        if (command.isBlank()) return
        _liveTranscript.value = command

        // If Live WebSocket is active, we can send text or it was already streamed via audio
        if (liveService.isConnected()) {
            _assistantState.value = AssistantVisualState.THINKING
            _statusMessage.value = "JARVIS is thinking..."
            viewModelScope.launch {
                repository.addMessage(role = "user", content = command)
                liveService.sendTextMessage(command)
            }
        } else {
            // Process command through Gemini REST + Function execution
            executeUserPrompt(command, isVoiceInput = true)
        }
    }

    private fun onAssistantFinishedSpeaking() {
        // AUTOMATICALLY LISTEN AGAIN in continuous calling mode!
        // Continuous hands-free loop: LISTEN -> THINK -> SPEAK -> LISTEN
        if (_callingModeActive.value && _continuousCallingEnabled.value) {
            _assistantState.value = AssistantVisualState.LISTENING
            _statusMessage.value = "Listening..."
            _liveTranscript.value = ""
            speechRecognizer.startListening(continuous = true)
        } else {
            _assistantState.value = AssistantVisualState.STANDBY
            _statusMessage.value = "JARVIS Standby"
        }
    }

    // -------------------------------------------------------------
    // Prompt & Tool Execution (Text & Voice)
    // -------------------------------------------------------------

    fun executeUserPrompt(prompt: String, isVoiceInput: Boolean = false) {
        if (prompt.isBlank()) return

        _assistantState.value = AssistantVisualState.THINKING
        _statusMessage.value = "JARVIS is thinking..."
        _liveTranscript.value = prompt

        thinkJob?.cancel()
        thinkJob = viewModelScope.launch(Dispatchers.IO) {
            repository.addMessage(role = "user", content = prompt)

            val effectiveKey = GeminiConfig.getEffectiveApiKey(_apiKey.value)
            val memContext = if (_memoryEnabled.value) repository.getFormattedMemoryContext() else ""
            val sysInst = GeminiConfig.getSystemInstruction(_personality.value, memContext)

            val result = restService.generateResponse(
                prompt = prompt,
                apiKey = effectiveKey,
                modelName = _textModel.value,
                systemInstruction = sysInst,
                onToolExecuted = { toolName, res ->
                    _statusMessage.value = "Tool $toolName: ${res.output}"
                }
            )

            when (result) {
                is RestResult.Success -> {
                    _statusMessage.value = "Response received"
                    _liveTranscript.value = result.text
                    repository.addMessage(
                        role = "assistant",
                        content = result.text,
                        toolName = result.executedTool,
                        toolResult = result.toolOutput
                    )

                    // Speak the response if in voice/calling mode or if requested
                    if (_callingModeActive.value || isVoiceInput) {
                        _assistantState.value = AssistantVisualState.SPEAKING
                        ttsManager.speak(result.text)
                    } else {
                        _assistantState.value = AssistantVisualState.STANDBY
                    }
                }
                is RestResult.Error -> {
                    _assistantState.value = AssistantVisualState.ERROR
                    _statusMessage.value = result.message
                    repository.addMessage(role = "system", content = "Error: ${result.message}")
                    if (_callingModeActive.value || isVoiceInput) {
                        ttsManager.speak("I encountered an issue: ${result.message}")
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------
    // Quick Actions
    // -------------------------------------------------------------

    fun runQuickAction(actionPrompt: String) {
        executeUserPrompt(actionPrompt, isVoiceInput = false)
    }

    // -------------------------------------------------------------
    // Memory & Notes Actions
    // -------------------------------------------------------------

    fun setNotesSearchQuery(query: String) {
        _notesSearchQuery.value = query
    }

    fun createNote(title: String, content: String) {
        viewModelScope.launch {
            repository.saveNote(title, content, source = "TEXT")
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            repository.deleteNote(id)
        }
    }

    fun clearAllNotes() {
        viewModelScope.launch {
            repository.clearNotes()
        }
    }

    fun saveMemory(category: String, key: String, value: String) {
        viewModelScope.launch {
            repository.saveMemory(category, key, value)
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            repository.deleteMemory(id)
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            repository.clearMemories()
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            repository.clearMessages()
        }
    }

    // -------------------------------------------------------------
    // Settings Actions
    // -------------------------------------------------------------

    fun saveApiKey(key: String) {
        val clean = key.trim()
        _apiKey.value = clean
        viewModelScope.launch {
            repository.setSetting("gemini_api_key", clean)
            _statusMessage.value = "API Key saved securely."
        }
    }

    fun deleteApiKey() {
        _apiKey.value = ""
        viewModelScope.launch {
            repository.deleteSetting("gemini_api_key")
            _statusMessage.value = "Stored API Key removed."
        }
    }

    fun testApiKey() {
        val effectiveKey = GeminiConfig.getEffectiveApiKey(_apiKey.value)
        _isTestingKey.value = true
        _testResult.value = null

        viewModelScope.launch {
            val result = restService.testApiKey(effectiveKey)
            _testResult.value = result
            _isTestingKey.value = false
        }
    }

    fun setLiveModel(model: String) {
        _liveModel.value = model
        viewModelScope.launch {
            repository.setSetting("live_model", model)
        }
    }

    fun setTextModel(model: String) {
        _textModel.value = model
        viewModelScope.launch {
            repository.setSetting("text_model", model)
        }
    }

    fun setVoiceName(voice: String) {
        _voiceName.value = voice
        viewModelScope.launch {
            repository.setSetting("live_voice", voice)
        }
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        _wakeWordEnabled.value = enabled
        speechRecognizer.setWakeWordConfig(enabled, _wakeWordPhrase.value)
        viewModelScope.launch {
            repository.setSetting("wake_word_enabled", enabled.toString())
        }
    }

    fun setWakeWordPhrase(phrase: String) {
        _wakeWordPhrase.value = phrase
        speechRecognizer.setWakeWordConfig(_wakeWordEnabled.value, phrase)
        viewModelScope.launch {
            repository.setSetting("wake_word_phrase", phrase)
        }
    }

    fun setWakeWordSensitivity(sensitivity: Float) {
        _wakeWordSensitivity.value = sensitivity
        viewModelScope.launch {
            repository.setSetting("wake_word_sensitivity", sensitivity.toString())
        }
    }

    fun setPersonality(mode: GeminiConfig.PersonalityMode) {
        _personality.value = mode
        viewModelScope.launch {
            repository.setSetting("personality_mode", mode.name)
        }
    }

    fun setMemoryEnabled(enabled: Boolean) {
        _memoryEnabled.value = enabled
        viewModelScope.launch {
            repository.setSetting("memory_enabled", enabled.toString())
        }
    }

    fun setContinuousCallingEnabled(enabled: Boolean) {
        _continuousCallingEnabled.value = enabled
        viewModelScope.launch {
            repository.setSetting("continuous_calling", enabled.toString())
        }
    }

    fun setBargeInEnabled(enabled: Boolean) {
        _bargeInEnabled.value = enabled
        viewModelScope.launch {
            repository.setSetting("barge_in_enabled", enabled.toString())
        }
    }

    fun setSpeechSpeed(speed: Float) {
        _speechSpeed.value = speed
        ttsManager.setSpeechRate(speed)
        viewModelScope.launch {
            repository.setSetting("speech_speed", speed.toString())
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioRecordManager.stopRecording()
        audioTrackPlayer.release()
        ttsManager.release()
        speechRecognizer.destroy()
        if (this::liveService.isInitialized) {
            liveService.disconnect("App closed")
        }
    }
}
