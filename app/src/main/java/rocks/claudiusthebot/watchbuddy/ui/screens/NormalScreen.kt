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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import rocks.claudiusthebot.watchbuddy.buddy.BuddyCanvas
import rocks.claudiusthebot.watchbuddy.state.BuddyUiState
import rocks.claudiusthebot.watchbuddy.state.Screen

@Composable
fun NormalScreen(ui: BuddyUiState, onNavigate: (Screen) -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { onNavigate(Screen.PET) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Top HUD: status pill + token count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (ui.connected) "●" else "○",
                    color = if (ui.connected) Color(0xFF4CAF50) else Color(0xFF666666),
                    fontSize = 10.sp
                )
                Text(
                    text = tokensLabel(ui.heartbeat.tokensToday),
                    color = Color(0xFFAAAAAA),
                    fontSize = 10.sp
                )
            }
            Spacer(Modifier.height(2.dp))

            // Buddy
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                BuddyCanvas(ui.state, ui.buddySpecies, Modifier.fillMaxSize())
            }

            Spacer(Modifier.height(4.dp))

            // Msg line (one line of status from the desktop)
            if (ui.heartbeat.msg.isNotBlank()) {
                Text(
                    text = ui.heartbeat.msg,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)
                )
            } else if (!ui.connected) {
                Text(
                    text = "Open Claude desktop → Developer → Hardware Buddy → Connect",
                    color = Color(0xFF888888),
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    maxLines = 3
                )
            }

            // Last conversation snippet — prefers real-time turn event over
            // periodic heartbeat entries snapshot. Shows what Claude most recently
            // said or did (tool calls shown as [tool_name]).
            val snippet = ui.lastTurn?.text
                ?: ui.heartbeat.entries.firstOrNull()?.takeIf { it.isNotBlank() }
            if (snippet != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = snippet,
                    color = Color(0xFF888888),
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)
                )
            }
        }

        // tap bottom-right → info
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp)
                .clickable { onNavigate(Screen.INFO) }
        ) {
            Text("i", color = Color(0xFF888888), fontSize = 10.sp)
        }
        // tap bottom-left → settings
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp)
                .clickable { onNavigate(Screen.SETTINGS) }
        ) {
            Text("⚙", color = Color(0xFF888888), fontSize = 10.sp)
        }
    }
}

private fun tokensLabel(tokens: Long): String {
    if (tokens <= 0) return ""
    return when {
        tokens >= 1_000_000L -> "${tokens / 1_000_000L}.${(tokens % 1_000_000L) / 100_000L}M"
        tokens >= 1_000L     -> "${tokens / 1_000L}k"
        else                 -> "$tokens"
    }
}
