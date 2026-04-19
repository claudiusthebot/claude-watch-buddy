package rocks.claudiusthebot.watchbuddy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import rocks.claudiusthebot.watchbuddy.ble.BuddyBleService
import rocks.claudiusthebot.watchbuddy.state.BuddyUiState

@Composable
fun ApprovalScreen(ui: BuddyUiState) {
    val p = ui.heartbeat.prompt ?: return

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Approve — upper half
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF1B5E20))
                .clickable {
                    BuddyBleService.instance?.sendDecision(p.id, "once")
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "APPROVE",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = p.tool.ifBlank { "tool" },
                    color = Color(0xFFB9F6CA),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
                if (p.hint.isNotBlank()) {
                    Text(
                        text = p.hint,
                        color = Color(0xFF80CBC4),
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                }
            }
        }
        // Deny — lower half
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFFB71C1C))
                .clickable {
                    BuddyBleService.instance?.sendDecision(p.id, "deny")
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "DENY",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

