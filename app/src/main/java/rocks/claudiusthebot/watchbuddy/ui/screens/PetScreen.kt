package rocks.claudiusthebot.watchbuddy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import rocks.claudiusthebot.watchbuddy.buddy.BuddyCanvas
import rocks.claudiusthebot.watchbuddy.state.BuddyUiState
import rocks.claudiusthebot.watchbuddy.state.Screen

@Composable
fun PetScreen(ui: BuddyUiState, onNavigate: (Screen) -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { onNavigate(Screen.INFO) }
    ) {
        Column(
            Modifier.fillMaxSize().padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "species ${ui.buddySpecies + 1}",
                color = Color(0xFF888888),
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                BuddyCanvas(
                    state = ui.state,
                    species = ui.buddySpecies,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
