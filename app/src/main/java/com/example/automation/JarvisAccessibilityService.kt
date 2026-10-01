package com.example.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class JarvisAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                AccessibilityEvent.TYPE_VIEW_CLICKED or
                AccessibilityEvent.TYPE_VIEW_FOCUSED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Track state updates if needed
    }

    override fun onInterrupt() {
        // Accessibility interrupted
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    fun readCurrentScreen(): ScreenContentSummary {
        val root = rootInActiveWindow ?: return ScreenContentSummary(
            packageName = "unknown",
            allTexts = emptyList(),
            interactiveElements = emptyList()
        )
        val texts = mutableListOf<String>()
        val interactives = mutableListOf<InteractiveElement>()

        traverseNode(root, texts, interactives)

        val pkgName = root.packageName?.toString() ?: "unknown"
        return ScreenContentSummary(
            packageName = pkgName,
            allTexts = texts.distinct(),
            interactiveElements = interactives
        )
    }

    private fun traverseNode(
        node: AccessibilityNodeInfo?,
        texts: MutableList<String>,
        interactives: MutableList<InteractiveElement>
    ) {
        if (node == null) return

        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()
        val label = when {
            !text.isNullOrBlank() -> text
            !desc.isNullOrBlank() -> desc
            else -> null
        }

        if (!label.isNullOrBlank()) {
            texts.add(label)
        }

        val isClickable = node.isClickable
        val isEditable = node.isEditable
        val className = node.className?.toString() ?: ""

        if (isClickable || isEditable || className.contains("Button", ignoreCase = true) || className.contains("EditText", ignoreCase = true)) {
            interactives.add(
                InteractiveElement(
                    text = label ?: "",
                    className = className,
                    viewId = node.viewIdResourceName,
                    isClickable = isClickable,
                    isEditable = isEditable
                )
            )
        }

        for (i in 0 until node.childCount) {
            traverseNode(node.getChild(i), texts, interactives)
        }
    }

    fun clickElement(target: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val found = findNodeByText(root, target)
        if (found != null) {
            var clickableNode: AccessibilityNodeInfo? = found
            while (clickableNode != null && !clickableNode.isClickable) {
                clickableNode = clickableNode.parent
            }
            val result = (clickableNode ?: found).performAction(AccessibilityNodeInfo.ACTION_CLICK)
            return result
        }
        return false
    }

    fun typeText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: findFirstEditable(root)
        if (focused != null) {
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        }
        return false
    }

    fun scroll(forward: Boolean): Boolean {
        val root = rootInActiveWindow ?: return false
        val scrollable = findScrollable(root)
        if (scrollable != null) {
            val action = if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            return scrollable.performAction(action)
        }
        return false
    }

    fun performGlobal(action: Int): Boolean {
        return performGlobalAction(action)
    }

    private fun findNodeByText(root: AccessibilityNodeInfo?, target: String): AccessibilityNodeInfo? {
        if (root == null) return null
        val text = root.text?.toString()
        val desc = root.contentDescription?.toString()
        if ((text != null && text.contains(target, ignoreCase = true)) ||
            (desc != null && desc.contains(target, ignoreCase = true))) {
            return root
        }
        for (i in 0 until root.childCount) {
            val childMatch = findNodeByText(root.getChild(i), target)
            if (childMatch != null) return childMatch
        }
        return null
    }

    private fun findFirstEditable(root: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (root == null) return null
        if (root.isEditable) return root
        for (i in 0 until root.childCount) {
            val child = findFirstEditable(root.getChild(i))
            if (child != null) return child
        }
        return null
    }

    private fun findScrollable(root: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (root == null) return null
        if (root.isScrollable) return root
        for (i in 0 until root.childCount) {
            val child = findScrollable(root.getChild(i))
            if (child != null) return child
        }
        return null
    }

    companion object {
        var instance: JarvisAccessibilityService? = null

        fun isEnabled(context: Context): Boolean {
            val serviceName = "${context.packageName}/${JarvisAccessibilityService::class.java.canonicalName}"
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabledServices.contains(serviceName)
        }

        fun openAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}

data class ScreenContentSummary(
    val packageName: String,
    val allTexts: List<String>,
    val interactiveElements: List<InteractiveElement>
)

data class InteractiveElement(
    val text: String,
    val className: String,
    val viewId: String?,
    val isClickable: Boolean,
    val isEditable: Boolean
)
