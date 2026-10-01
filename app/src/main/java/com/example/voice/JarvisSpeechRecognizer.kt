package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class JarvisSpeechRecognizer(
    private val context: Context,
    private val onSpeechResult: (String) -> Unit,
    private val onSpeechPartialResult: (String) -> Unit,
    private val onSpeechRmsLevel: (Float) -> Unit,
    private val onSpeechStopCommand: () -> Unit,
    private val onSpeechWakeWordDetected: () -> Unit,
    private val onSpeechError: (Int) -> Unit
) {
    private var recognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isListening = false
    private var continuousMode = false
    private var wakeWordEnabled = true
    private var targetWakeWord = "jarvis"

    private val restartRunnable = Runnable {
        if (continuousMode && !isListening) {
            startListening(continuous = true)
        }
    }

    fun setWakeWordConfig(enabled: Boolean, phrase: String) {
        wakeWordEnabled = enabled
        targetWakeWord = phrase.lowercase().trim()
    }

    fun startListening(continuous: Boolean = true) {
        continuousMode = continuous
        mainHandler.post {
            mainHandler.removeCallbacks(restartRunnable)
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                isListening = false
                return@post
            }
            ensureRecognizer()
            if (!isListening && recognizer != null) {
                try {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString())
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                    }
                    recognizer?.startListening(intent)
                    isListening = true
                } catch (e: Exception) {
                    isListening = false
                }
            }
        }
    }

    fun stopListening() {
        continuousMode = false
        isListening = false
        mainHandler.post {
            mainHandler.removeCallbacks(restartRunnable)
            try {
                recognizer?.stopListening()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun cancel() {
        continuousMode = false
        isListening = false
        mainHandler.post {
            mainHandler.removeCallbacks(restartRunnable)
            try {
                recognizer?.cancel()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    private fun ensureRecognizer() {
        if (recognizer == null) {
            try {
                if (SpeechRecognizer.isRecognitionAvailable(context)) {
                    recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(createListener())
                    }
                }
            } catch (e: Exception) {
                recognizer = null
                isListening = false
            }
        }
    }

    private fun restartListeningWithDelay(delayMs: Long = 400) {
        if (!continuousMode) return
        mainHandler.removeCallbacks(restartRunnable)
        mainHandler.postDelayed(restartRunnable, delayMs)
    }

    private fun createListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            isListening = true
        }

        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(rmsdB: Float) {
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            onSpeechRmsLevel(normalized)
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            isListening = false
        }

        override fun onError(errorCode: Int) {
            isListening = false
            onSpeechError(errorCode)

            // Avoid rapid restarts on permanent/fatal errors
            val shouldRestart = when (errorCode) {
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> false
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                    // Reset recognizer if stuck busy
                    try {
                        recognizer?.cancel()
                    } catch (e: Exception) {
                        // ignore
                    }
                    true
                }
                else -> true
            }

            if (shouldRestart) {
                val delay = if (errorCode == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) 800L else 500L
                restartListeningWithDelay(delay)
            }
        }

        override fun onResults(results: Bundle?) {
            isListening = false
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val spoken = matches?.firstOrNull()?.trim() ?: ""

            if (spoken.isNotBlank()) {
                val lower = spoken.lowercase()

                // Check Stop command
                if (lower == "stop" || lower.contains("jarvis, stop") || lower.contains("jarvis stop")) {
                    onSpeechStopCommand()
                } else if (wakeWordEnabled && (lower.contains("wake up") || lower.startsWith(targetWakeWord) || lower.startsWith("hey $targetWakeWord"))) {
                    onSpeechWakeWordDetected()
                    // Strip the wake word prefix and pass the remaining command if present
                    val cleanCommand = spoken
                        .replaceFirst(Regex("^(hey\\s+)?$targetWakeWord[\\s,]*", RegexOption.IGNORE_CASE), "")
                        .trim()
                    if (cleanCommand.isNotBlank()) {
                        onSpeechResult(cleanCommand)
                    }
                } else {
                    onSpeechResult(spoken)
                }
            }

            restartListeningWithDelay(350)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val partials = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = partials?.firstOrNull()?.trim() ?: ""
            if (text.isNotBlank()) {
                val lower = text.lowercase()
                if (lower == "stop" || lower.contains("jarvis, stop") || lower.contains("jarvis stop")) {
                    onSpeechStopCommand()
                } else {
                    onSpeechPartialResult(text)
                }
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun destroy() {
        continuousMode = false
        isListening = false
        mainHandler.post {
            mainHandler.removeCallbacks(restartRunnable)
            try {
                recognizer?.destroy()
            } catch (e: Exception) {
                // ignore
            }
            recognizer = null
        }
    }
}
