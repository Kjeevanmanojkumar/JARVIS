package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class TtsManager(
    context: Context,
    private val onStart: () -> Unit,
    private val onDone: () -> Unit
) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setPitch(0.95f)
                tts?.setSpeechRate(1.05f)
                isInitialized = true

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        onStart()
                    }

                    override fun onDone(utteranceId: String?) {
                        onDone()
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        onDone()
                    }
                })
            }
        }
    }

    fun speak(text: String, utteranceId: String = "jarvis_response") {
        if (!isInitialized || text.isBlank()) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun setSpeechRate(rate: Float) {
        tts?.setSpeechRate(rate.coerceIn(0.5f, 2.0f))
    }

    fun stop() {
        tts?.stop()
        onDone()
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
