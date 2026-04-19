package rocks.claudiusthebot.watchbuddy.buddy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import rocks.claudiusthebot.watchbuddy.state.BuddyState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Very simple per-state buddy drawing. Three styles: Blob (species 0), Cat
 * (species 1), Robot (species 2). All render on a Canvas so they scale to
 * whatever size Compose gives us.
 */
@Composable
fun BuddyCanvas(
    state: BuddyState,
    species: Int,
    modifier: Modifier = Modifier
) {
    // Animation tick
    var tick by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis { now -> tick = now }
        }
    }

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawBuddy(state, species, tick)
        }
    }
}

private fun DrawScope.drawBuddy(state: BuddyState, species: Int, t: Long) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = minOf(size.width, size.height) * 0.28f

    // Breathing / wobble offset
    val breathe = sin((t % 4000L) / 4000.0 * 2 * PI).toFloat()
    val wobble  = sin((t % 600L) / 600.0 * 2 * PI).toFloat()

    val accent = when (species) {
        1 -> Color(0xFFF7B955) // cat
        2 -> Color(0xFF6FB1FC) // robot
        else -> Color(0xFFD97757) // blob
    }

    // attention pulse halo
    if (state == BuddyState.ATTENTION) {
        val alpha = 0.5f + 0.5f * sin((t % 700L) / 700.0 * 2 * PI).toFloat()
        drawCircle(
            color = Color(0xFFF44336).copy(alpha = 0.4f * alpha),
            radius = r * 1.55f,
            center = androidx.compose.ui.geometry.Offset(cx, cy)
        )
    }

    val bodyY = cy + breathe * 2f
    val wobbleX = if (state == BuddyState.DIZZY) wobble * 6f else 0f

    // Body (head)
    drawCircle(
        color = accent,
        radius = r,
        center = androidx.compose.ui.geometry.Offset(cx + wobbleX, bodyY)
    )

    // Eyes per state
    val eyeY = bodyY - r * 0.15f
    val eyeDx = r * 0.35f
    val eyeR = r * 0.12f

    when (state) {
        BuddyState.SLEEP -> {
            // closed eyes — short horizontal lines
            drawLineEye(cx - eyeDx + wobbleX, eyeY, eyeR * 1.5f)
            drawLineEye(cx + eyeDx + wobbleX, eyeY, eyeR * 1.5f)
            // Zzz
            drawZzz(cx + r * 0.7f, bodyY - r * 0.7f, r * 0.18f, t)
        }
        BuddyState.DIZZY -> {
            // spiral eyes — shift with wobble
            drawSpiralEye(cx - eyeDx + wobbleX, eyeY, eyeR * 1.1f, t)
            drawSpiralEye(cx + eyeDx + wobbleX, eyeY, eyeR * 1.1f, t)
        }
        BuddyState.HEART -> {
            drawHeartEye(cx - eyeDx, eyeY, eyeR)
            drawHeartEye(cx + eyeDx, eyeY, eyeR)
            drawFloatingHearts(cx, cy, r, t)
        }
        BuddyState.BUSY -> {
            // focused — small dots + sweat drop
            drawCircle(Color.Black, eyeR * 0.7f, androidx.compose.ui.geometry.Offset(cx - eyeDx, eyeY))
            drawCircle(Color.Black, eyeR * 0.7f, androidx.compose.ui.geometry.Offset(cx + eyeDx, eyeY))
            val sweatY = (t % 1200L) / 1200f
            drawCircle(
                color = Color(0xFF6FB1FC),
                radius = eyeR * 0.6f,
                center = androidx.compose.ui.geometry.Offset(cx + r * 0.9f, eyeY + r * 0.6f * sweatY)
            )
        }
        BuddyState.CELEBRATE -> {
            drawStarEye(cx - eyeDx, eyeY, eyeR, t)
            drawStarEye(cx + eyeDx, eyeY, eyeR, t)
            drawConfetti(cx, cy, r, t)
        }
        BuddyState.ATTENTION -> {
            // wide open eyes with bright pupils
            drawCircle(Color.White, eyeR, androidx.compose.ui.geometry.Offset(cx - eyeDx, eyeY))
            drawCircle(Color.White, eyeR, androidx.compose.ui.geometry.Offset(cx + eyeDx, eyeY))
            drawCircle(Color.Black, eyeR * 0.55f, androidx.compose.ui.geometry.Offset(cx - eyeDx, eyeY))
            drawCircle(Color.Black, eyeR * 0.55f, androidx.compose.ui.geometry.Offset(cx + eyeDx, eyeY))
        }
        BuddyState.IDLE -> {
            val blink = ((t / 120L) % 40L) == 0L
            if (blink) {
                drawLineEye(cx - eyeDx, eyeY, eyeR * 1.5f)
                drawLineEye(cx + eyeDx, eyeY, eyeR * 1.5f)
            } else {
                drawCircle(Color.Black, eyeR, androidx.compose.ui.geometry.Offset(cx - eyeDx, eyeY))
                drawCircle(Color.Black, eyeR, androidx.compose.ui.geometry.Offset(cx + eyeDx, eyeY))
            }
        }
    }

    // Mouth / smile
    if (state != BuddyState.DIZZY && state != BuddyState.SLEEP) {
        val mouthY = bodyY + r * 0.25f
        drawArc(
            color = Color.Black,
            startAngle = 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(cx - r * 0.35f + wobbleX, mouthY - r * 0.25f),
            size = androidx.compose.ui.geometry.Size(r * 0.7f, r * 0.45f),
            style = Stroke(width = r * 0.08f)
        )
    }

    // Species accessories
    when (species) {
        1 -> {
            // Cat ears
            val earR = r * 0.25f
            val pathTop = bodyY - r * 0.9f
            drawCircle(accent, earR, androidx.compose.ui.geometry.Offset(cx - r * 0.7f + wobbleX, pathTop))
            drawCircle(accent, earR, androidx.compose.ui.geometry.Offset(cx + r * 0.7f + wobbleX, pathTop))
        }
        2 -> {
            // Robot antenna
            drawLine(
                color = Color.White,
                start = androidx.compose.ui.geometry.Offset(cx + wobbleX, bodyY - r * 0.9f),
                end   = androidx.compose.ui.geometry.Offset(cx + wobbleX, bodyY - r * 1.35f),
                strokeWidth = r * 0.06f
            )
            drawCircle(Color(0xFFF44336), r * 0.1f,
                androidx.compose.ui.geometry.Offset(cx + wobbleX, bodyY - r * 1.35f))
        }
    }
}

private fun DrawScope.drawLineEye(x: Float, y: Float, w: Float) {
    drawLine(
        color = Color.Black,
        start = androidx.compose.ui.geometry.Offset(x - w / 2f, y),
        end   = androidx.compose.ui.geometry.Offset(x + w / 2f, y),
        strokeWidth = w * 0.25f
    )
}

private fun DrawScope.drawSpiralEye(x: Float, y: Float, r: Float, t: Long) {
    val steps = 18
    val base = (t % 900L) / 900f * 2f * PI.toFloat()
    for (i in 0 until steps) {
        val ang = base + i * 0.4f
        val rr = r * (i / steps.toFloat())
        val x1 = x + rr * cos(ang)
        val y1 = y + rr * sin(ang)
        drawCircle(Color.Black, r * 0.06f,
            androidx.compose.ui.geometry.Offset(x1, y1))
    }
}

private fun DrawScope.drawHeartEye(x: Float, y: Float, r: Float) {
    val c = Color(0xFFEE2233)
    val off = r * 0.45f
    drawCircle(c, r * 0.6f, androidx.compose.ui.geometry.Offset(x - off, y - r * 0.1f))
    drawCircle(c, r * 0.6f, androidx.compose.ui.geometry.Offset(x + off, y - r * 0.1f))
    // triangle-ish bottom via path not needed — use overlapping circle + rect
    drawCircle(c, r * 0.7f, androidx.compose.ui.geometry.Offset(x, y + r * 0.3f))
}

private fun DrawScope.drawStarEye(x: Float, y: Float, r: Float, t: Long) {
    val pulse = 0.7f + 0.3f * sin((t % 500L) / 500.0 * 2 * PI).toFloat()
    drawCircle(Color(0xFFFFD54F), r * pulse, androidx.compose.ui.geometry.Offset(x, y))
}

private fun DrawScope.drawFloatingHearts(cx: Float, cy: Float, r: Float, t: Long) {
    for (i in 0 until 4) {
        val phase = (t / 30L + i * 20L) % 80L
        val x = cx + (i - 2) * r * 0.6f
        val y = cy - r * 0.6f - phase * r * 0.02f
        val a = 1f - phase / 80f
        drawCircle(
            color = Color(0xFFEE2233).copy(alpha = a.coerceIn(0f, 1f)),
            radius = r * 0.12f,
            center = androidx.compose.ui.geometry.Offset(x, y)
        )
    }
}

private fun DrawScope.drawConfetti(cx: Float, cy: Float, r: Float, t: Long) {
    val colors = listOf(
        Color(0xFFF7B955), Color(0xFF6FB1FC), Color(0xFFEE2233), Color(0xFF4CAF50)
    )
    for (i in 0 until 10) {
        val phase = ((t / 30L + i * 37L) % 60L).toFloat() / 60f
        val angle = i * (2 * PI / 10).toFloat()
        val dist = r * (0.6f + phase * 1.2f)
        val x = cx + dist * cos(angle)
        val y = cy + dist * sin(angle)
        drawCircle(
            color = colors[i % colors.size].copy(alpha = (1f - phase).coerceIn(0f, 1f)),
            radius = r * 0.08f,
            center = androidx.compose.ui.geometry.Offset(x, y)
        )
    }
}

private fun DrawScope.drawZzz(x: Float, y: Float, r: Float, t: Long) {
    val phase = (t / 30L) % 60L
    val rise = phase / 60f
    drawLine(
        color = Color.White.copy(alpha = (1f - rise).coerceIn(0f, 1f)),
        start = androidx.compose.ui.geometry.Offset(x, y - rise * r * 2f),
        end   = androidx.compose.ui.geometry.Offset(x + r, y - rise * r * 2f),
        strokeWidth = r * 0.25f
    )
    drawLine(
        color = Color.White.copy(alpha = (1f - rise).coerceIn(0f, 1f)),
        start = androidx.compose.ui.geometry.Offset(x + r, y - rise * r * 2f),
        end   = androidx.compose.ui.geometry.Offset(x, y + r * 0.8f - rise * r * 2f),
        strokeWidth = r * 0.25f
    )
    drawLine(
        color = Color.White.copy(alpha = (1f - rise).coerceIn(0f, 1f)),
        start = androidx.compose.ui.geometry.Offset(x, y + r * 0.8f - rise * r * 2f),
        end   = androidx.compose.ui.geometry.Offset(x + r, y + r * 0.8f - rise * r * 2f),
        strokeWidth = r * 0.25f
    )
}
