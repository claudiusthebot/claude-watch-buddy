package rocks.claudiusthebot.watchbuddy.state

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import rocks.claudiusthebot.watchbuddy.protocol.Heartbeat

private val Context.dataStore by preferencesDataStore(name = "buddy_settings")

object Prefs {
    val NAME = stringPreferencesKey("name")
    val OWNER = stringPreferencesKey("owner")
    val SPECIES = intPreferencesKey("species")
    val APPR = intPreferencesKey("appr")
    val DENY = intPreferencesKey("deny")
    val NAP = intPreferencesKey("nap")
    val VEL = intPreferencesKey("vel")
    val LVL = intPreferencesKey("lvl")
    val LAST_APPROVED_MS = longPreferencesKey("last_approved_ms")
}

/**
 * Singleton in-memory UI state + persistence layer.
 * BLE service and Compose UI both hold a reference to the same instance.
 */
class BuddyStore(private val appContext: Context) {

    private val _state = MutableStateFlow(loadInitial())
    val state = _state.asStateFlow()

    val uptimeMs: Long get() = System.currentTimeMillis() - bootMs
    private val bootMs = System.currentTimeMillis()

    private fun loadInitial(): BuddyUiState = runBlocking {
        val prefs = appContext.dataStore.data.first()
        BuddyUiState(
            deviceName = prefs[Prefs.NAME] ?: defaultName(),
            ownerName = prefs[Prefs.OWNER] ?: "",
            buddySpecies = prefs[Prefs.SPECIES] ?: 0,
            stats = Stats(
                approvals = prefs[Prefs.APPR] ?: 0,
                denies = prefs[Prefs.DENY] ?: 0,
                naps = prefs[Prefs.NAP] ?: 0,
                velocity = prefs[Prefs.VEL] ?: 0,
                level = prefs[Prefs.LVL] ?: 0,
                lastApprovedAtMs = prefs[Prefs.LAST_APPROVED_MS] ?: 0L
            )
        )
    }

    private fun defaultName(): String {
        // Random 4-digit suffix like the ESP32 firmware.
        val n = (1000 + (Math.random() * 9000).toInt())
        return "Claude-$n"
    }

    fun update(transform: (BuddyUiState) -> BuddyUiState) {
        _state.value = transform(_state.value)
    }

    fun setConnected(connected: Boolean, encrypted: Boolean = false) {
        update { it.copy(connected = connected, encrypted = encrypted) }
        recomputeState()
    }

    fun onHeartbeat(hb: Heartbeat) {
        update { it.copy(heartbeat = hb, lastEventMs = System.currentTimeMillis()) }
        recomputeState()
    }

    fun setScreen(screen: Screen) {
        update { it.copy(screen = screen) }
    }

    fun setName(name: String) {
        update { it.copy(deviceName = name) }
        runBlocking { appContext.dataStore.edit { it[Prefs.NAME] = name } }
    }

    fun setOwner(name: String) {
        update { it.copy(ownerName = name) }
        runBlocking { appContext.dataStore.edit { it[Prefs.OWNER] = name } }
    }

    fun setSpecies(idx: Int) {
        update { it.copy(buddySpecies = idx) }
        runBlocking { appContext.dataStore.edit { it[Prefs.SPECIES] = idx } }
    }

    fun recordApproval(fastMs: Long) {
        update {
            val s = it.stats.copy(
                approvals = it.stats.approvals + 1,
                lastApprovedAtMs = System.currentTimeMillis(),
                velocity = if (fastMs in 1..4999) it.stats.velocity + 1 else it.stats.velocity
            )
            it.copy(stats = s)
        }
        persistStats()
        if (fastMs in 1..4999) flash(BuddyState.HEART, 1800)
    }

    fun recordDeny() {
        update { it.copy(stats = it.stats.copy(denies = it.stats.denies + 1)) }
        persistStats()
    }

    fun recordNap() {
        update { it.copy(stats = it.stats.copy(naps = it.stats.naps + 1)) }
        persistStats()
    }

    fun recordShake() {
        flash(BuddyState.DIZZY, 1500)
    }

    fun markLevelUp() {
        flash(BuddyState.CELEBRATE, 2500)
    }

    fun clearAllStats() {
        update { it.copy(stats = Stats()) }
        persistStats()
    }

    private var flashUntil = 0L
    private var flashState: BuddyState? = null

    private fun flash(s: BuddyState, forMs: Long) {
        flashState = s
        flashUntil = System.currentTimeMillis() + forMs
        recomputeState()
    }

    fun recomputeState() {
        val now = System.currentTimeMillis()
        val ui = _state.value

        val next = when {
            flashState != null && now < flashUntil -> flashState!!
            !ui.connected -> BuddyState.SLEEP
            ui.heartbeat.prompt != null -> BuddyState.ATTENTION
            ui.heartbeat.running > 0 -> BuddyState.BUSY
            else -> BuddyState.IDLE
        }
        if (flashState != null && now >= flashUntil) flashState = null
        if (ui.state != next) update { it.copy(state = next) }
    }

    private fun persistStats() {
        val s = _state.value.stats
        runBlocking {
            appContext.dataStore.edit { p ->
                p[Prefs.APPR] = s.approvals
                p[Prefs.DENY] = s.denies
                p[Prefs.NAP] = s.naps
                p[Prefs.VEL] = s.velocity
                p[Prefs.LVL] = s.level
                p[Prefs.LAST_APPROVED_MS] = s.lastApprovedAtMs
            }
        }
    }

    companion object {
        @Volatile private var INSTANCE: BuddyStore? = null
        fun get(context: Context): BuddyStore = INSTANCE ?: synchronized(this) {
            INSTANCE ?: BuddyStore(context.applicationContext).also { INSTANCE = it }
        }
    }
}
