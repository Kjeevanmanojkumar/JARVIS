package com.example.ai

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String? = null,
    @Json(name = "inlineData") val inlineData: GeminiInlineData? = null,
    @Json(name = "functionCall") val functionCall: GeminiFunctionCall? = null,
    @Json(name = "functionResponse") val functionResponse: GeminiFunctionResponse? = null
)

@JsonClass(generateAdapter = true)
data class GeminiInlineData(
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class GeminiFunctionCall(
    @Json(name = "name") val name: String,
    @Json(name = "args") val args: Map<String, Any?>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiFunctionResponse(
    @Json(name = "name") val name: String,
    @Json(name = "response") val response: Map<String, Any?>
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "role") val role: String? = "user",
    @Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiToolDeclaration(
    @Json(name = "functionDeclarations") val functionDeclarations: List<GeminiFunctionDef>
)

@JsonClass(generateAdapter = true)
data class GeminiFunctionDef(
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String,
    @Json(name = "parameters") val parameters: GeminiSchema? = null
)

@JsonClass(generateAdapter = true)
data class GeminiSchema(
    @Json(name = "type") val type: String = "OBJECT",
    @Json(name = "properties") val properties: Map<String, GeminiSchemaProperty>? = null,
    @Json(name = "required") val required: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiSchemaProperty(
    @Json(name = "type") val type: String,
    @Json(name = "description") val description: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiRestRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null,
    @Json(name = "tools") val tools: List<GeminiToolDeclaration>? = null,
    @Json(name = "generationConfig") val generationConfig: Map<String, Any?>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiRestResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>? = null,
    @Json(name = "error") val error: GeminiApiError? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent? = null,
    @Json(name = "finishReason") val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiApiError(
    @Json(name = "code") val code: Int? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "status") val status: String? = null
)
