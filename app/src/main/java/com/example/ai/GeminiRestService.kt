package com.example.ai

import com.example.automation.JarvisTools
import com.example.automation.ToolExecutionResult
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class GeminiRestService(
    private val tools: JarvisTools
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val requestAdapter = moshi.adapter(GeminiRestRequest::class.java)
    private val responseAdapter = moshi.adapter(GeminiRestResponse::class.java)

    suspend fun generateResponse(
        prompt: String,
        apiKey: String,
        modelName: String = GeminiConfig.DEFAULT_TEXT_MODEL,
        systemInstruction: String = "",
        conversationHistory: List<GeminiContent> = emptyList(),
        onToolExecuted: ((toolName: String, result: ToolExecutionResult) -> Unit)? = null
    ): RestResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext RestResult.Error("API Key is missing. Please configure it in Settings.")
        }

        val cleanModel = if (modelName.startsWith("models/")) modelName.substringAfter("models/") else modelName
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent?key=$apiKey"

        val contents = ArrayList<GeminiContent>()
        contents.addAll(conversationHistory)
        contents.add(
            GeminiContent(
                role = "user",
                parts = listOf(GeminiPart(text = prompt))
            )
        )

        val sysContent = if (systemInstruction.isNotBlank()) {
            GeminiContent(role = "system", parts = listOf(GeminiPart(text = systemInstruction)))
        } else null

        val toolDecls = tools.getToolDeclarations()

        val requestPayload = GeminiRestRequest(
            contents = contents,
            systemInstruction = sysContent,
            tools = toolDecls,
            generationConfig = mapOf(
                "temperature" to 0.7f,
                "maxOutputTokens" to 1024
            )
        )

        try {
            val jsonBody = requestAdapter.toJson(requestPayload)
            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext RestResult.Error("HTTP ${response.code}: $responseString")
            }

            val parsed = responseAdapter.fromJson(responseString)
            val candidate = parsed?.candidates?.firstOrNull()
            val firstPart = candidate?.content?.parts?.firstOrNull()

            // Check if model called a tool
            if (firstPart?.functionCall != null) {
                val call = firstPart.functionCall
                val toolName = call.name
                val toolArgs = call.args

                val execResult = tools.executeTool(toolName, toolArgs)
                onToolExecuted?.invoke(toolName, execResult)

                // Follow up with tool response to complete user command
                val followUpContents = ArrayList(contents)
                followUpContents.add(
                    GeminiContent(
                        role = "model",
                        parts = listOf(firstPart)
                    )
                )
                followUpContents.add(
                    GeminiContent(
                        role = "function",
                        parts = listOf(
                            GeminiPart(
                                functionResponse = GeminiFunctionResponse(
                                    name = toolName,
                                    response = mapOf(
                                        "success" to execResult.success,
                                        "output" to execResult.output
                                    )
                                )
                            )
                        )
                    )
                )

                val followUpPayload = GeminiRestRequest(
                    contents = followUpContents,
                    systemInstruction = sysContent,
                    generationConfig = mapOf("temperature" to 0.7f)
                )

                val followUpJson = requestAdapter.toJson(followUpPayload)
                val followUpReq = Request.Builder()
                    .url(url)
                    .post(followUpJson.toRequestBody("application/json".toMediaType()))
                    .build()

                val followUpResp = client.newCall(followUpReq).execute()
                val followUpStr = followUpResp.body?.string() ?: ""
                val followUpParsed = responseAdapter.fromJson(followUpStr)
                val finalAnswer = followUpParsed?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: execResult.output

                return@withContext RestResult.Success(
                    text = finalAnswer,
                    executedTool = toolName,
                    toolOutput = execResult.output
                )
            }

            val text = firstPart?.text ?: "Task processed."
            RestResult.Success(text = text)
        } catch (e: Exception) {
            RestResult.Error("Error connecting to Gemini: ${e.message}")
        }
    }

    suspend fun testApiKey(apiKey: String): ConnectionTestResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext ConnectionTestResult(false, "API Key is empty.")
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val requestPayload = GeminiRestRequest(
            contents = listOf(
                GeminiContent(role = "user", parts = listOf(GeminiPart(text = "Hello JARVIS")))
            )
        )

        try {
            val jsonBody = requestAdapter.toJson(requestPayload)
            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val code = response.code
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                ConnectionTestResult(true, "API Key is valid and active!")
            } else {
                val reason = when (code) {
                    400 -> "Bad Request. The API key might be invalid or formatted incorrectly."
                    403 -> "Permission Denied. The API key does not have Gemini API permissions or is restricted."
                    404 -> "Model not found."
                    429 -> "Quota Exceeded. API rate limits or billing restrictions reached."
                    else -> "Connection failed (HTTP $code): $body"
                }
                ConnectionTestResult(false, reason)
            }
        } catch (e: Exception) {
            ConnectionTestResult(false, "Network error: ${e.message}")
        }
    }
}

sealed class RestResult {
    data class Success(val text: String, val executedTool: String? = null, val toolOutput: String? = null) : RestResult()
    data class Error(val message: String) : RestResult()
}

data class ConnectionTestResult(
    val isSuccess: Boolean,
    val message: String
)
