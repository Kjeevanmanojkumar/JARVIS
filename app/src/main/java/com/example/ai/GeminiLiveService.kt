package com.example.ai

import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiLiveService(
    private val scope: CoroutineScope,
    private val onConnected: () -> Unit,
    private val onDisconnected: (String?) -> Unit,
    private val onAudioChunkReceived: (ByteArray) -> Unit,
    private val onTranscriptReceived: (String, Boolean) -> Unit, // text, isUser
    private val onInterrupted: () -> Unit,
    private val onTurnComplete: () -> Unit,
    private val onToolCallReceived: (callId: String, functionName: String, args: Map<String, Any?>) -> Unit,
    private val onError: (String) -> Unit
) {
    private var webSocket: WebSocket? = null
    private var isConnected = false
    private var pingJob: Job? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // infinite for WebSocket
        .writeTimeout(20, TimeUnit.SECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    fun connect(
        apiKey: String,
        modelName: String = GeminiConfig.LIVE_MODEL_FALLBACK,
        voiceName: String = "Puck",
        systemInstruction: String = ""
    ) {
        if (apiKey.isBlank()) {
            onError("Gemini API Key is missing. Please configure it in Settings.")
            return
        }

        disconnect("Starting new session")

        val cleanModel = if (modelName.startsWith("models/")) modelName else "models/$modelName"
        val url = "wss://${GeminiConfig.WEBSOCKET_HOST}${GeminiConfig.WEBSOCKET_PATH}?key=$apiKey"
        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                isConnected = true
                onConnected()

                // Send setup frame
                sendSetupFrame(ws, cleanModel, voiceName, systemInstruction)
            }

            override fun onMessage(ws: WebSocket, text: String) {
                handleServerMessage(text)
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                ws.close(1000, null)
                isConnected = false
                onDisconnected(reason)
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                isConnected = false
                onDisconnected(reason)
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                val errorMsg = t.message ?: "Connection failed"
                onError("Live connection error: $errorMsg")
                onDisconnected(errorMsg)
            }
        })
    }

    private fun sendSetupFrame(
        ws: WebSocket,
        model: String,
        voiceName: String,
        systemInstruction: String
    ) {
        try {
            val setupJson = JSONObject().apply {
                val setupObj = JSONObject().apply {
                    put("model", model)
                    val genConfig = JSONObject().apply {
                        val modalities = JSONArray().apply {
                            put("AUDIO")
                            put("TEXT")
                        }
                        put("responseModalities", modalities)
                        val speechConfig = JSONObject().apply {
                            val voiceConfig = JSONObject().apply {
                                val prebuilt = JSONObject().apply {
                                    put("voiceName", voiceName)
                                }
                                put("prebuiltVoiceConfig", prebuilt)
                            }
                            put("voiceConfig", voiceConfig)
                        }
                        put("speechConfig", speechConfig)
                    }
                    put("generationConfig", genConfig)

                    if (systemInstruction.isNotBlank()) {
                        val sysInst = JSONObject().apply {
                            val parts = JSONArray().apply {
                                put(JSONObject().put("text", systemInstruction))
                            }
                            put("parts", parts)
                        }
                        put("systemInstruction", sysInst)
                    }
                }
                put("setup", setupObj)
            }
            ws.send(setupJson.toString())
        } catch (e: Exception) {
            onError("Failed to send setup frame: ${e.message}")
        }
    }

    fun sendTextMessage(text: String) {
        if (!isConnected || webSocket == null) {
            onError("Cannot send message: JARVIS Live is not connected.")
            return
        }

        try {
            val messageJson = JSONObject().apply {
                val clientContent = JSONObject().apply {
                    val turns = JSONArray().apply {
                        val turn = JSONObject().apply {
                            put("role", "user")
                            val parts = JSONArray().apply {
                                put(JSONObject().put("text", text))
                            }
                            put("parts", parts)
                        }
                        put(turn)
                    }
                    put("turns", turns)
                    put("turnComplete", true)
                }
                put("clientContent", clientContent)
            }
            webSocket?.send(messageJson.toString())
        } catch (e: Exception) {
            onError("Error sending text: ${e.message}")
        }
    }

    fun sendAudioChunk(pcmChunk: ByteArray) {
        if (!isConnected || webSocket == null) return

        try {
            val base64Data = Base64.encodeToString(pcmChunk, Base64.NO_WRAP)
            val audioJson = JSONObject().apply {
                val realtimeInput = JSONObject().apply {
                    val mediaChunks = JSONArray().apply {
                        val chunk = JSONObject().apply {
                            put("mimeType", "audio/pcm;rate=16000")
                            put("data", base64Data)
                        }
                        put(chunk)
                    }
                    put("mediaChunks", mediaChunks)
                }
                put("realtimeInput", realtimeInput)
            }
            webSocket?.send(audioJson.toString())
        } catch (e: Exception) {
            // ignore packet drop
        }
    }

    fun sendToolResponse(callId: String, functionName: String, result: String) {
        if (!isConnected || webSocket == null) return
        try {
            val toolRespJson = JSONObject().apply {
                val toolResponse = JSONObject().apply {
                    val functionResponses = JSONArray().apply {
                        val resp = JSONObject().apply {
                            put("id", callId)
                            put("name", functionName)
                            val responseObj = JSONObject().apply {
                                put("result", result)
                            }
                            put("response", responseObj)
                        }
                        put(resp)
                    }
                    put("functionResponses", functionResponses)
                }
                put("toolResponse", toolResponse)
            }
            webSocket?.send(toolRespJson.toString())
        } catch (e: Exception) {
            onError("Error sending tool response: ${e.message}")
        }
    }

    private fun handleServerMessage(text: String) {
        try {
            val root = JSONObject(text)

            if (root.has("serverContent")) {
                val serverContent = root.getJSONObject("serverContent")

                if (serverContent.optBoolean("interrupted", false)) {
                    onInterrupted()
                }

                if (serverContent.has("modelTurn")) {
                    val modelTurn = serverContent.getJSONObject("modelTurn")
                    val parts = modelTurn.optJSONArray("parts")
                    if (parts != null) {
                        for (i in 0 until parts.length()) {
                            val part = parts.getJSONObject(i)
                            if (part.has("text")) {
                                onTranscriptReceived(part.getString("text"), false)
                            }
                            if (part.has("inlineData")) {
                                val inline = part.getJSONObject("inlineData")
                                val dataBase64 = inline.getString("data")
                                val pcmBytes = Base64.decode(dataBase64, Base64.NO_WRAP)
                                onAudioChunkReceived(pcmBytes)
                            }
                        }
                    }
                }

                if (serverContent.optBoolean("turnComplete", false)) {
                    onTurnComplete()
                }
            }

            if (root.has("toolCall")) {
                val toolCall = root.getJSONObject("toolCall")
                val functionCalls = toolCall.optJSONArray("functionCalls")
                if (functionCalls != null && functionCalls.length() > 0) {
                    val fc = functionCalls.getJSONObject(0)
                    val callId = fc.optString("id", "call_${System.currentTimeMillis()}")
                    val name = fc.getString("name")
                    val argsObj = fc.optJSONObject("args")
                    val argsMap = mutableMapOf<String, Any?>()
                    if (argsObj != null) {
                        for (key in argsObj.keys()) {
                            argsMap[key] = argsObj.get(key)
                        }
                    }
                    onToolCallReceived(callId, name, argsMap)
                }
            }
        } catch (e: Exception) {
            // Non-critical parsing frame
        }
    }

    fun isConnected(): Boolean = isConnected

    fun disconnect(reason: String = "User requested disconnect") {
        isConnected = false
        try {
            webSocket?.close(1000, reason)
        } catch (e: Exception) {
            // ignore
        }
        webSocket = null
        pingJob?.cancel()
        pingJob = null
    }
}
