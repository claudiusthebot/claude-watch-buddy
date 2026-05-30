package rocks.claudiusthebot.watchbuddy.state

import rocks.claudiusthebot.watchbuddy.protocol.Heartbeat

/** Animation state — mirrors the ESP32 firmware's seven states. */
enum class BuddyState { SLEEP, IDLE, BUSY, ATTENTION, CELEBRATE, DIZZY, HEART }

/** Screens the user can switch between with the crown / side button. */
enum class Screen { NORMAL, PET, INFO, APPROVAL, SETTINGS }

/** Persistent stats backed by DataStore. */
data class Stats(
    val approvals: Int = 0,
    val denies: Int = 0,
    val naps: Int = 0,
    val velocity: Int = 0,
    val level: Int = 0,
    val lastApprovedAtMs: Long = 0L
) {
    fun level(tokens: Long): Int = (tokens / 50_000L).toInt()
}

/**
 * Last completed conversation turn from the desktop.
 * Updated in real-time via turn events (role=assistant or user).
 */
data class LastTurn(
    val role: String,   // "assistant" or "user"
    val text: String    // extracted text (tool names for tool_use blocks)
)

/** Full UI state the Compose layer observes. */
data class BuddyUiState(
    val connected: Boolean = false,
    val encrypted: Boolean = false,
    val deviceName: String = "Clawd",
    val ownerName: String = "",
    val buddySpecies: Int = 0,
    val state: BuddyState = BuddyState.SLEEP,
    val heartbeat: Heartbeat = Heartbeat(),
    val screen: Screen = Screen.NORMAL,
    val stats: Stats = Stats(),
    val lastEventMs: Long = 0L,
    val lastTurn: LastTurn? = null,
    // BLE status — surfaced on the Info screen so the user can tell what's wrong
    val advertising: Boolean = false,
    val advertisingError: String? = null,
    val btEnabled: Boolean = true,
    val missingPermissions: List<String> = emptyList()
)
