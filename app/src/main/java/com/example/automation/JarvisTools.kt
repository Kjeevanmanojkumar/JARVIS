package com.example.automation

import android.accessibilityservice.AccessibilityService
import android.content.Context
import com.example.ai.GeminiFunctionDef
import com.example.ai.GeminiSchema
import com.example.ai.GeminiSchemaProperty
import com.example.ai.GeminiToolDeclaration
import com.example.data.local.JarvisRepository

class JarvisTools(
    private val context: Context,
    private val repository: JarvisRepository,
    private val automationManager: PhoneAutomationManager
) {
    fun getToolDeclarations(): List<GeminiToolDeclaration> {
        val functionDefs = listOf(
            GeminiFunctionDef(
                name = "open_app",
                description = "Open an installed Android app by its name, such as YouTube, Chrome, Settings, Camera, Maps, WhatsApp, etc.",
                parameters = GeminiSchema(
                    properties = mapOf(
                        "app_name" to GeminiSchemaProperty(type = "STRING", description = "The name of the app to launch")
                    ),
                    required = listOf("app_name")
                )
            ),
            GeminiFunctionDef(
                name = "navigate",
                description = "Perform a system navigation action on the phone: 'home', 'back', 'recents', or 'notifications'.",
                parameters = GeminiSchema(
                    properties = mapOf(
                        "action" to GeminiSchemaProperty(type = "STRING", description = "'home', 'back', 'recents', or 'notifications'")
                    ),
                    required = listOf("action")
                )
            ),
            GeminiFunctionDef(
                name = "read_screen",
                description = "Read the current visible screen content, text, and interactive buttons using the Accessibility Service.",
                parameters = GeminiSchema(
                    properties = emptyMap(),
                    required = emptyList()
                )
            ),
            GeminiFunctionDef(
                name = "click_screen",
                description = "Click on a visible UI element or button on the screen matching the given target label or text.",
                parameters = GeminiSchema(
                    properties = mapOf(
                        "target" to GeminiSchemaProperty(type = "STRING", description = "The label or text of the button or element to tap")
                    ),
                    required = listOf("target")
                )
            ),
            GeminiFunctionDef(
                name = "type_screen",
                description = "Type text into the currently focused or active input field on screen.",
                parameters = GeminiSchema(
                    properties = mapOf(
                        "text" to GeminiSchemaProperty(type = "STRING", description = "The text to input into the field")
                    ),
                    required = listOf("text")
                )
            ),
            GeminiFunctionDef(
                name = "scroll_screen",
                description = "Scroll the current screen content up or down.",
                parameters = GeminiSchema(
                    properties = mapOf(
                        "direction" to GeminiSchemaProperty(type = "STRING", description = "'forward' (down) or 'backward' (up)")
                    ),
                    required = listOf("direction")
                )
            ),
            GeminiFunctionDef(
                name = "create_note",
                description = "Create and save a note into JARVIS memory.",
                parameters = GeminiSchema(
                    properties = mapOf(
                        "title" to GeminiSchemaProperty(type = "STRING", description = "Brief title of the note"),
                        "content" to GeminiSchemaProperty(type = "STRING", description = "Full note content")
                    ),
                    required = listOf("title", "content")
                )
            ),
            GeminiFunctionDef(
                name = "remember_fact",
                description = "Save an important piece of user information, preference, or fact into JARVIS long-term memory.",
                parameters = GeminiSchema(
                    properties = mapOf(
                        "fact" to GeminiSchemaProperty(type = "STRING", description = "The fact or preference to remember"),
                        "category" to GeminiSchemaProperty(type = "STRING", description = "'PREFERENCE', 'FACT', or 'CUSTOM'")
                    ),
                    required = listOf("fact")
                )
            ),
            GeminiFunctionDef(
                name = "open_setting",
                description = "Open a specific Android settings screen (e.g. 'wifi', 'bluetooth', 'sound', 'display', 'battery').",
                parameters = GeminiSchema(
                    properties = mapOf(
                        "setting_name" to GeminiSchemaProperty(type = "STRING", description = "Name of the setting")
                    ),
                    required = listOf("setting_name")
                )
            ),
            GeminiFunctionDef(
                name = "adjust_volume",
                description = "Adjust device volume ('up', 'down', 'mute', 'unmute').",
                parameters = GeminiSchema(
                    properties = mapOf(
                        "direction" to GeminiSchemaProperty(type = "STRING", description = "'up', 'down', 'mute', or 'unmute'")
                    ),
                    required = listOf("direction")
                )
            ),
            GeminiFunctionDef(
                name = "web_search",
                description = "Search the web using Google Search.",
                parameters = GeminiSchema(
                    properties = mapOf(
                        "query" to GeminiSchemaProperty(type = "STRING", description = "The search query")
                    ),
                    required = listOf("query")
                )
            ),
            GeminiFunctionDef(
                name = "make_call",
                description = "Prepare a phone call to a given contact name or phone number.",
                parameters = GeminiSchema(
                    properties = mapOf(
                        "contact" to GeminiSchemaProperty(type = "STRING", description = "Phone number or contact name")
                    ),
                    required = listOf("contact")
                )
            ),
            GeminiFunctionDef(
                name = "send_message",
                description = "Prepare an SMS text message to a contact with a message.",
                parameters = GeminiSchema(
                    properties = mapOf(
                        "contact" to GeminiSchemaProperty(type = "STRING", description = "Recipient contact or phone number"),
                        "message" to GeminiSchemaProperty(type = "STRING", description = "Body of the message")
                    ),
                    required = listOf("contact", "message")
                )
            )
        )
        return listOf(GeminiToolDeclaration(functionDeclarations = functionDefs))
    }

    suspend fun executeTool(name: String, args: Map<String, Any?>?): ToolExecutionResult {
        return try {
            when (name) {
                "open_app" -> {
                    val appName = args?.get("app_name")?.toString() ?: ""
                    val res = automationManager.openAppByName(appName)
                    ToolExecutionResult(res is AutomationResult.Success, res.message)
                }
                "navigate" -> {
                    val actionStr = args?.get("action")?.toString()?.lowercase() ?: ""
                    val service = JarvisAccessibilityService.instance
                    if (service == null) {
                        ToolExecutionResult(false, "Accessibility Service is not enabled. Please enable it in Settings.")
                    } else {
                        val actionInt = when (actionStr) {
                            "home" -> AccessibilityService.GLOBAL_ACTION_HOME
                            "back" -> AccessibilityService.GLOBAL_ACTION_BACK
                            "recents" -> AccessibilityService.GLOBAL_ACTION_RECENTS
                            "notifications" -> AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS
                            else -> AccessibilityService.GLOBAL_ACTION_HOME
                        }
                        val success = service.performGlobal(actionInt)
                        ToolExecutionResult(success, if (success) "Navigated $actionStr" else "Failed to navigate $actionStr")
                    }
                }
                "read_screen" -> {
                    val service = JarvisAccessibilityService.instance
                    if (service == null) {
                        ToolExecutionResult(false, "Accessibility Service is disabled. Cannot read screen.")
                    } else {
                        val summary = service.readCurrentScreen()
                        val textList = summary.allTexts.take(20).joinToString("; ")
                        val buttons = summary.interactiveElements.filter { it.text.isNotBlank() }.take(10).joinToString(", ") { it.text }
                        val resultStr = "App: ${summary.packageName}\nText visible: $textList\nInteractive buttons: $buttons"
                        ToolExecutionResult(true, resultStr)
                    }
                }
                "click_screen" -> {
                    val target = args?.get("target")?.toString() ?: ""
                    val service = JarvisAccessibilityService.instance
                    if (service == null) {
                        ToolExecutionResult(false, "Accessibility Service disabled.")
                    } else {
                        val clicked = service.clickElement(target)
                        ToolExecutionResult(clicked, if (clicked) "Tapped '$target'" else "Element '$target' not found on screen")
                    }
                }
                "type_screen" -> {
                    val text = args?.get("text")?.toString() ?: ""
                    val service = JarvisAccessibilityService.instance
                    if (service == null) {
                        ToolExecutionResult(false, "Accessibility Service disabled.")
                    } else {
                        val typed = service.typeText(text)
                        ToolExecutionResult(typed, if (typed) "Typed '$text'" else "No focused text field found to type")
                    }
                }
                "scroll_screen" -> {
                    val dir = args?.get("direction")?.toString()?.lowercase() ?: "forward"
                    val service = JarvisAccessibilityService.instance
                    if (service == null) {
                        ToolExecutionResult(false, "Accessibility Service disabled.")
                    } else {
                        val scrolled = service.scroll(dir == "forward" || dir == "down")
                        ToolExecutionResult(scrolled, if (scrolled) "Scrolled $dir" else "Cannot scroll screen")
                    }
                }
                "create_note" -> {
                    val title = args?.get("title")?.toString() ?: "Note"
                    val content = args?.get("content")?.toString() ?: ""
                    repository.saveNote(title, content, source = "VOICE")
                    ToolExecutionResult(true, "Note '$title' saved successfully.")
                }
                "remember_fact" -> {
                    val fact = args?.get("fact")?.toString() ?: ""
                    val category = args?.get("category")?.toString() ?: "FACT"
                    repository.saveMemory(category = category, key = "User Fact", value = fact)
                    ToolExecutionResult(true, "Stored in long-term memory: '$fact'")
                }
                "open_setting" -> {
                    val settingName = args?.get("setting_name")?.toString() ?: "settings"
                    val res = automationManager.openSystemSetting(settingName)
                    ToolExecutionResult(res is AutomationResult.Success, res.message)
                }
                "adjust_volume" -> {
                    val dir = args?.get("direction")?.toString() ?: "up"
                    val res = automationManager.adjustVolume(dir)
                    ToolExecutionResult(res is AutomationResult.Success, res.message)
                }
                "web_search" -> {
                    val query = args?.get("query")?.toString() ?: ""
                    val res = automationManager.performWebSearch(query)
                    ToolExecutionResult(res is AutomationResult.Success, res.message)
                }
                "make_call" -> {
                    val contact = args?.get("contact")?.toString() ?: ""
                    val res = automationManager.prepareCall(contact)
                    ToolExecutionResult(res is AutomationResult.Success, res.message)
                }
                "send_message" -> {
                    val contact = args?.get("contact")?.toString() ?: ""
                    val message = args?.get("message")?.toString() ?: ""
                    val res = automationManager.prepareSms(contact, message)
                    ToolExecutionResult(res is AutomationResult.Success, res.message)
                }
                else -> ToolExecutionResult(false, "Unknown tool: $name")
            }
        } catch (e: Exception) {
            ToolExecutionResult(false, "Tool error: ${e.message}")
        }
    }
}

data class ToolExecutionResult(
    val success: Boolean,
    val output: String
)
