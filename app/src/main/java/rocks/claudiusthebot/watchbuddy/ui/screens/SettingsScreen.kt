package rocks.claudiusthebot.watchbuddy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import rocks.claudiusthebot.watchbuddy.state.BuddyStore
import rocks.claudiusthebot.watchbuddy.state.BuddyUiState
import rocks.claudiusthebot.watchbuddy.state.Screen

@Composable
fun SettingsScreen(
    ui: BuddyUiState,
    store: BuddyStore,
    onNavigate: (Screen) -> Unit
) {
    val scroll = rememberScrollState()
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(scroll)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "settings",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))

        // cycle species
        SettingsRow("ascii pet", "→ ${ui.buddySpecies + 1}") {
            store.setSpecies((ui.buddySpecies + 1) % 3)
        }
        SettingsRow("reset stats", "") {
            store.clearAllStats()
        }
        SettingsRow("close", "×") {
            onNavigate(Screen.NORMAL)
        }
    }
}

@Composable
private fun SettingsRow(label: String, value: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1E1E))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.White, fontSize = 11.sp)
            if (value.isNotEmpty()) {
                Text(value, color = Color(0xFFD97757), fontSize = 11.sp, textAlign = TextAlign.End)
            }
        }
    }
}
