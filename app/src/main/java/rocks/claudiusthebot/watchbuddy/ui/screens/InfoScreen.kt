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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import rocks.claudiusthebot.watchbuddy.state.BuddyUiState
import rocks.claudiusthebot.watchbuddy.state.Screen

@Composable
fun InfoScreen(ui: BuddyUiState, onNavigate: (Screen) -> Unit) {
    val scroll = rememberScrollState()
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { onNavigate(Screen.NORMAL) }
            .padding(10.dp)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = ui.deviceName,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            if (ui.ownerName.isNotBlank()) {
                Text(
                    text = "owner: ${ui.ownerName}",
                    color = Color(0xFF999999),
                    fontSize = 10.sp
                )
            }
            Spacer(Modifier.height(6.dp))
            Row2("connected", if (ui.connected) "yes" else "no")
            Row2("encrypted", if (ui.encrypted) "yes" else "no")
            Row2("tokens", "${ui.heartbeat.tokens}")
            Row2("today",  "${ui.heartbeat.tokensToday}")
            Row2("running", "${ui.heartbeat.running}")
            Row2("waiting", "${ui.heartbeat.waiting}")
            Row2("appr",    "${ui.stats.approvals}")
            Row2("deny",    "${ui.stats.denies}")
            Row2("vel",     "${ui.stats.velocity}")
            Row2("level",   "${ui.stats.level(ui.heartbeat.tokens)}")
        }
    }
}

@Composable
private fun Row2(label: String, value: String) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
    ) {
        Text(label, color = Color(0xFF666666), fontSize = 10.sp, modifier = Modifier.weight(1f))
        Text(
            value,
            color = Color.White,
            fontSize = 10.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}
