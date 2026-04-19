package rocks.claudiusthebot.watchbuddy.ble

import java.util.UUID

/**
 * Nordic UART Service — the BLE transport the Claude desktop apps speak.
 *
 * Service:       6e400001-b5a3-f393-e0a9-e50e24dcca9e
 * RX (write):    6e400002-b5a3-f393-e0a9-e50e24dcca9e   (desktop -> watch)
 * TX (notify):   6e400003-b5a3-f393-e0a9-e50e24dcca9e   (watch  -> desktop)
 */
object NusConstants {
    val SERVICE: UUID   = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
    val RX_CHAR: UUID   = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
    val TX_CHAR: UUID   = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
    val CCCD: UUID      = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    /** Name prefix the desktop picker filters on. */
    const val NAME_PREFIX = "Claude"
}
