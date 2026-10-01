package com.example.ai

import com.example.BuildConfig

object GeminiConfig {
    const val DEFAULT_LIVE_MODEL = "gemini-3.8-live"
    const val LIVE_MODEL_FALLBACK = "gemini-2.5-flash-native-audio-preview-12-2025"
    const val DEFAULT_TEXT_MODEL = "gemini-3.5-flash"
    const val WEBSOCKET_HOST = "generativelanguage.googleapis.com"
    const val WEBSOCKET_PATH = "/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent"

    val AVAILABLE_VOICES = listOf("Puck", "Charon", "Kore", "Fenrir", "Aoede")

    enum class PersonalityMode(val displayName: String, val promptModifier: String) {
        NORMAL(
            displayName = "Normal",
            promptModifier = "You are JARVIS, a highly intelligent, proactive, and respectful futuristic AI assistant. Your responses should be clear, elegant, helpful, and concise."
        ),
        SERIOUS(
            displayName = "Serious",
            promptModifier = "You are JARVIS operating in Tactical / Serious protocol. Keep all communications strictly concise, mission-focused, authoritative, and direct. Zero fluff."
        ),
        COMPANION(
            displayName = "Companion",
            promptModifier = "You are JARVIS, the personal AI companion reminiscent of Tony Stark's iconic system: dry British wit, subtly humorous, loyal, sophisticated, and brilliant."
        )
    }

    fun getSystemInstruction(
        personality: PersonalityMode,
        memoryContext: String = ""
    ): String {
        return buildString {
            append(personality.promptModifier)
            append("\n\nYou are operating directly on the user's Android device as an integrated voice and automation intelligence.")
            append("\nYou have direct access to tools for phone control, note-taking, memory, accessibility, and screen actions.")
            append("\nAlways be ready to execute actions or speak concise replies.")
            append("\nIf the user says 'JARVIS, stop' or 'stop', acknowledge immediately and halt.")
            if (memoryContext.isNotBlank()) {
                append("\n\n")
                append(memoryContext)
            }
        }
    }

    fun getEffectiveApiKey(userKey: String?): String {
        val trimmed = userKey?.trim()
        if (!trimmed.isNullOrEmpty() && trimmed != "MY_GEMINI_API_KEY") {
            return trimmed
        }
        val buildKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }
        return if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") buildKey else ""
    }
}
