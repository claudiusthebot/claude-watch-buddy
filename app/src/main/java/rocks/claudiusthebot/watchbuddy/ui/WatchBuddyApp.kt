package rocks.claudiusthebot.watchbuddy.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import rocks.claudiusthebot.watchbuddy.state.BuddyStore
import rocks.claudiusthebot.watchbuddy.state.Screen
import rocks.claudiusthebot.watchbuddy.ui.screens.ApprovalScreen
import rocks.claudiusthebot.watchbuddy.ui.screens.InfoScreen
import rocks.claudiusthebot.watchbuddy.ui.screens.NormalScreen
import rocks.claudiusthebot.watchbuddy.ui.screens.PetScreen
import rocks.claudiusthebot.watchbuddy.ui.screens.SettingsScreen

@Composable
fun WatchBuddyApp() {
    MaterialTheme {
        val context = LocalContext.current
        val store = remember { BuddyStore.get(context) }
        val ui by store.state.collectAsState()

        // Auto-switch to Approval screen when a prompt arrives, and back when cleared.
        LaunchedEffect(ui.heartbeat.prompt?.id) {
            if (ui.heartbeat.prompt != null) {
                store.setScreen(Screen.APPROVAL)
            } else if (ui.screen == Screen.APPROVAL) {
                store.setScreen(Screen.NORMAL)
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(Modifier.fillMaxSize()) {
                when (ui.screen) {
                    Screen.NORMAL   -> NormalScreen(ui) { store.setScreen(it) }
                    Screen.PET      -> PetScreen(ui) { next -> store.setScreen(next) }
                    Screen.INFO     -> InfoScreen(ui) { next -> store.setScreen(next) }
                    Screen.APPROVAL -> ApprovalScreen(ui)
                    Screen.SETTINGS -> SettingsScreen(ui, store) { store.setScreen(it) }
                }
            }
        }
    }
}
