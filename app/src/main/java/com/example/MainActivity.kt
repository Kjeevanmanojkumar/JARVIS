package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.JarvisTab
import com.example.ui.JarvisViewModel
import com.example.ui.screens.CallingModeScreen
import com.example.ui.screens.ChatTerminalScreen
import com.example.ui.screens.HudMainScreen
import com.example.ui.screens.MemoryNotesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTheme

class MainActivity : ComponentActivity() {

    private val viewModel: JarvisViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            JarvisTheme {
                MainContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainContent(viewModel: JarvisViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val callingModeActive by viewModel.callingModeActive.collectAsStateWithLifecycle()

    // Request Audio & Notification permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Permissions granted/denied handled
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val needed = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    // Refresh accessibility status on resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkAccessibilityStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = JarvisBackground,
        bottomBar = {
            if (!callingModeActive) {
                JarvisBottomBar(
                    currentTab = currentTab,
                    onSelectTab = { viewModel.setTab(it) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(
                targetState = if (callingModeActive) JarvisTab.CALLING else currentTab,
                label = "tab_crossfade"
            ) { tab ->
                when (tab) {
                    JarvisTab.HUD -> HudMainScreen(viewModel = viewModel)
                    JarvisTab.CALLING -> CallingModeScreen(viewModel = viewModel)
                    JarvisTab.TERMINAL -> ChatTerminalScreen(viewModel = viewModel)
                    JarvisTab.MEMORY_NOTES -> MemoryNotesScreen(viewModel = viewModel)
                    JarvisTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun JarvisBottomBar(
    currentTab: JarvisTab,
    onSelectTab: (JarvisTab) -> Unit
) {
    val items = listOf(
        NavTabItem(JarvisTab.HUD, "HUD", Icons.Default.Dashboard, "tab_hud"),
        NavTabItem(JarvisTab.CALLING, "CALL", Icons.Default.Call, "tab_calling"),
        NavTabItem(JarvisTab.TERMINAL, "CHAT", Icons.Default.Chat, "tab_terminal"),
        NavTabItem(JarvisTab.MEMORY_NOTES, "DATA", Icons.Default.FolderShared, "tab_memory"),
        NavTabItem(JarvisTab.SETTINGS, "CONFIG", Icons.Default.Settings, "tab_settings")
    )

    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        containerColor = JarvisSurface,
        tonalElevation = 0.dp
    ) {
        items.forEach { item ->
            val isSelected = currentTab == item.tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelectTab(item.tab) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(20.dp)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF001E2B),
                    selectedTextColor = JarvisCyan,
                    indicatorColor = JarvisCyan,
                    unselectedIconColor = JarvisTextMuted,
                    unselectedTextColor = JarvisTextMuted
                ),
                modifier = Modifier.testTag(item.testTag)
            )
        }
    }
}

data class NavTabItem(
    val tab: JarvisTab,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)
