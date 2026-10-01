package com.example.automation

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.provider.Settings

class PhoneAutomationManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    fun openAppByName(appName: String): AutomationResult {
        val query = appName.trim().lowercase()

        // Known common mappings
        val knownPackages = mapOf(
            "youtube" to "com.google.android.youtube",
            "chrome" to "com.android.chrome",
            "browser" to "com.android.chrome",
            "maps" to "com.google.android.apps.maps",
            "camera" to "com.google.android.GoogleCamera",
            "settings" to "com.android.settings",
            "whatsapp" to "com.whatsapp",
            "play store" to "com.android.vending",
            "gmail" to "com.google.android.gm",
            "clock" to "com.google.android.deskclock",
            "calculator" to "com.google.android.calculator",
            "calendar" to "com.google.android.calendar",
            "spotify" to "com.spotify.music"
        )

        for ((key, pkg) in knownPackages) {
            if (query.contains(key)) {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return AutomationResult.Success("Launched $appName ($pkg)")
                }
            }
        }

        // Try searching installed applications by label
        try {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (appInfo in packages) {
                val label = pm.getApplicationLabel(appInfo).toString().lowercase()
                if (label.contains(query) || query.contains(label)) {
                    val intent = pm.getLaunchIntentForPackage(appInfo.packageName)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        return AutomationResult.Success("Launched ${pm.getApplicationLabel(appInfo)}")
                    }
                }
            }
        } catch (e: Exception) {
            // fallback
        }

        // Generic intent fallbacks
        if (query.contains("camera")) {
            val intent = Intent("android.media.action.STILL_IMAGE_CAMERA").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                return AutomationResult.Success("Opened Camera")
            }
        }

        if (query.contains("setting")) {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return AutomationResult.Success("Opened Settings")
        }

        return AutomationResult.Failure("App '$appName' was not found on this device.")
    }

    fun openSystemSetting(settingType: String): AutomationResult {
        val action = when (settingType.lowercase().trim()) {
            "wifi", "wi-fi" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "sound", "volume" -> Settings.ACTION_SOUND_SETTINGS
            "display", "screen" -> Settings.ACTION_DISPLAY_SETTINGS
            "battery" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
            "apps" -> Settings.ACTION_APPLICATION_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }
        val intent = Intent(action).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return AutomationResult.Success("Opened $settingType settings.")
    }

    fun adjustVolume(direction: String): AutomationResult {
        if (audioManager == null) return AutomationResult.Failure("Audio manager not available")
        val dir = when (direction.lowercase().trim()) {
            "up", "raise", "increase" -> AudioManager.ADJUST_RAISE
            "down", "lower", "decrease" -> AudioManager.ADJUST_LOWER
            "mute" -> AudioManager.ADJUST_MUTE
            "unmute" -> AudioManager.ADJUST_UNMUTE
            else -> AudioManager.ADJUST_SAME
        }
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, dir, AudioManager.FLAG_SHOW_UI)
        return AutomationResult.Success("Volume adjusted ($direction).")
    }

    fun performWebSearch(query: String): AutomationResult {
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            AutomationResult.Success("Searching web for '$query'")
        } catch (e: Exception) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
            AutomationResult.Success("Opened search in browser for '$query'")
        }
    }

    fun prepareCall(contactOrNumber: String): AutomationResult {
        val uri = if (contactOrNumber.all { it.isDigit() || it == '+' || it == '-' || it == ' ' }) {
            Uri.parse("tel:${contactOrNumber.replace(" ", "")}")
        } else {
            Uri.parse("tel:")
        }
        val intent = Intent(Intent.ACTION_DIAL, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return AutomationResult.Success("Prepared phone call to $contactOrNumber.")
    }

    fun prepareSms(contactOrNumber: String, message: String): AutomationResult {
        val uri = Uri.parse("smsto:${contactOrNumber.filter { it.isDigit() || it == '+' }}")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return AutomationResult.Success("Prepared SMS to $contactOrNumber: '$message'")
    }

    fun openUrl(url: String): AutomationResult {
        var formatted = url.trim()
        if (!formatted.startsWith("http://") && !formatted.startsWith("https://")) {
            formatted = "https://$formatted"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formatted)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return AutomationResult.Success("Navigating to $formatted")
    }
}

sealed class AutomationResult(val message: String) {
    class Success(message: String) : AutomationResult(message)
    class Failure(message: String) : AutomationResult(message)
}
