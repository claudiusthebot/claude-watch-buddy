package rocks.claudiusthebot.watchbuddy.ble

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.ParcelUuid
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONArray
import rocks.claudiusthebot.watchbuddy.MainActivity
import rocks.claudiusthebot.watchbuddy.R
import rocks.claudiusthebot.watchbuddy.protocol.BuddyMessages
import rocks.claudiusthebot.watchbuddy.protocol.BuddyProtocol
import rocks.claudiusthebot.watchbuddy.protocol.Heartbeat
import rocks.claudiusthebot.watchbuddy.state.BuddyStore
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Foreground service that runs the BLE GATT server + Nordic UART peripheral.
 * The watch advertises as "Claude-XXXX"; the Claude desktop connects and pushes
 * newline-delimited JSON. We decode, update the store, and ack / reply.
 */
class BuddyBleService : Service(), BuddyProtocol.Listener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private lateinit var store: BuddyStore
    private lateinit var protocol: BuddyProtocol

    private var btManager: BluetoothManager? = null
    private var advertiser: BluetoothLeAdvertiser? = null
    private var gattServer: BluetoothGattServer? = null
    private var txChar: BluetoothGattCharacteristic? = null
    private var rxChar: BluetoothGattCharacteristic? = null

    // subscribers get notify packets; usually just one at a time (the desktop)
    private val subscribers = HashSet<String>()
    private var connectedDevice: BluetoothDevice? = null
    private var mtu: Int = 23

    // file receive state (Folder push protocol)
    private var currentCharName: String? = null
    private var currentFile: String? = null
    private var currentFileSize: Long = 0L
    private var currentFileWritten: Long = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        store = BuddyStore.get(this)
        protocol = BuddyProtocol(this)
        startForegroundWithNotif()
        startBle()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopBle()
        scope.cancel()
        if (instance === this) instance = null
    }

    // ------------------------------------------------------------
    // BLE lifecycle
    // ------------------------------------------------------------

    @SuppressLint("MissingPermission")
    private fun startBle() {
        btManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter: BluetoothAdapter? = btManager?.adapter
        if (adapter == null) {
            Log.e(TAG, "no Bluetooth adapter")
            return
        }
        if (!adapter.isEnabled) {
            Log.w(TAG, "Bluetooth is disabled")
            // We don't force-enable — user must toggle it. Service keeps running.
        }
        if (!hasBlePermissions()) {
            Log.e(TAG, "missing BLE runtime permissions — waiting for user to grant")
            return
        }

        adapter.name = store.state.value.deviceName

        gattServer = btManager?.openGattServer(this, gattCallback)
        gattServer?.clearServices()

        val service = BluetoothGattService(
            NusConstants.SERVICE,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )
        rxChar = BluetoothGattCharacteristic(
            NusConstants.RX_CHAR,
            BluetoothGattCharacteristic.PROPERTY_WRITE or
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        txChar = BluetoothGattCharacteristic(
            NusConstants.TX_CHAR,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ
        ).apply {
            addDescriptor(
                BluetoothGattDescriptor(
                    NusConstants.CCCD,
                    BluetoothGattDescriptor.PERMISSION_READ or
                        BluetoothGattDescriptor.PERMISSION_WRITE
                )
            )
        }

        service.addCharacteristic(rxChar)
        service.addCharacteristic(txChar)
        gattServer?.addService(service)

        advertiser = adapter.bluetoothLeAdvertiser
        startAdvertising()
    }

    @SuppressLint("MissingPermission")
    private fun startAdvertising() {
        val adv = advertiser ?: return
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setConnectable(true)
            .setTimeout(0)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addServiceUuid(ParcelUuid(NusConstants.SERVICE))
            .build()

        adv.startAdvertising(settings, data, advertiseCallback)
    }

    @SuppressLint("MissingPermission")
    private fun stopBle() {
        try { advertiser?.stopAdvertising(advertiseCallback) } catch (_: Exception) {}
        try { gattServer?.close() } catch (_: Exception) {}
        gattServer = null
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.i(TAG, "advertising started")
        }
        override fun onStartFailure(errorCode: Int) {
            Log.e(TAG, "advertising failed err=$errorCode")
        }
    }

    // ------------------------------------------------------------
    // GATT callbacks
    // ------------------------------------------------------------

    private val gattCallback = object : BluetoothGattServerCallback() {

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "connected: ${device.address}")
                    connectedDevice = device
                    store.setConnected(true, encrypted = false)
                    updateNotif(connected = true, attention = false)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i(TAG, "disconnected: ${device.address}")
                    subscribers.remove(device.address)
                    if (connectedDevice?.address == device.address) connectedDevice = null
                    store.setConnected(false)
                    updateNotif(connected = false, attention = false)
                    // restart advertising so a fresh connect is possible
                    try {
                        advertiser?.stopAdvertising(advertiseCallback)
                        startAdvertising()
                    } catch (e: Exception) {
                        Log.w(TAG, "restart advertise err", e)
                    }
                }
            }
        }

        override fun onMtuChanged(device: BluetoothDevice?, mtuSize: Int) {
            mtu = mtuSize
            Log.i(TAG, "mtu changed to $mtuSize")
        }

        @SuppressLint("MissingPermission")
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            if (characteristic.uuid == NusConstants.RX_CHAR) {
                protocol.onBytes(value, value.size)
            }
            if (responseNeeded) {
                gattServer?.sendResponse(
                    device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null
                )
            }
        }

        @SuppressLint("MissingPermission")
        override fun onDescriptorWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            if (descriptor.uuid == NusConstants.CCCD) {
                if (value.isNotEmpty() && value[0].toInt() != 0) {
                    subscribers.add(device.address)
                } else {
                    subscribers.remove(device.address)
                }
            }
            if (responseNeeded) {
                gattServer?.sendResponse(
                    device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null
                )
            }
        }
    }

    // ------------------------------------------------------------
    // Protocol listener
    // ------------------------------------------------------------

    override fun onHeartbeat(hb: Heartbeat) {
        val prev = store.state.value.heartbeat.prompt?.id
        store.onHeartbeat(hb)
        if (hb.prompt != null && hb.prompt.id != prev) {
            lastPromptAtMs = System.currentTimeMillis()
        }
        updateNotif(connected = true, attention = hb.prompt != null)
    }

    override fun onTurnEvent(role: String, content: JSONArray) {
        // Noop for now — could surface summaries on screen.
    }

    override fun onTime(epochSec: Long, tzOffsetSec: Int) {
        // Android has its own system clock — just log it.
        Log.d(TAG, "time sync epoch=$epochSec tz=$tzOffsetSec")
    }

    override fun onOwner(name: String) {
        store.setOwner(name)
        sendAck("owner", true)
    }

    override fun onSetName(name: String) {
        store.setName(name)
        // Adapter name change requires renaming the BluetoothAdapter; already done
        // on next advertiser restart.
        sendAck("name", true)
    }

    override fun onUnpair() {
        // Android doesn't expose a clean "forget all bonds for this app". We
        // ack and let the framework handle pairing state per device.
        sendAck("unpair", true)
    }

    override fun onStatusRequest() {
        val ui = store.state.value
        val json = BuddyMessages.statusAck(
            name = ui.deviceName,
            sec = ui.encrypted,
            batPct = getBatteryPct(), batMv = null, batMa = null, usb = isOnCharger(),
            upSec = store.uptimeMs / 1000L,
            heapBytes = Runtime.getRuntime().freeMemory(),
            appr = ui.stats.approvals,
            deny = ui.stats.denies,
            vel = ui.stats.velocity,
            nap = ui.stats.naps,
            lvl = ui.stats.level(ui.heartbeat.tokens)
        )
        enqueue(json.toByteArray(Charsets.UTF_8))
    }

    override fun onCharBegin(name: String, total: Long) {
        currentCharName = name
        Log.i(TAG, "char_begin name=$name total=$total")
        sendAck("char_begin", true)
    }

    override fun onFile(path: String, size: Long) {
        // Validate path (no absolute, no ..)
        if (path.startsWith("/") || path.contains("..")) {
            sendAck("file", false, error = "bad path")
            return
        }
        currentFile = path
        currentFileSize = size
        currentFileWritten = 0L
        sendAck("file", true)
    }

    override fun onChunk(dataB64: String) {
        val bytes = try {
            android.util.Base64.decode(dataB64, android.util.Base64.DEFAULT)
        } catch (e: Exception) {
            sendAck("chunk", false, error = "bad base64")
            return
        }
        currentFileWritten += bytes.size
        // We discard the bytes — character packs not supported in v1.
        sendAck("chunk", true, n = currentFileWritten)
    }

    override fun onFileEnd() {
        val n = currentFileWritten
        currentFile = null
        sendAck("file_end", true, n = n)
    }

    override fun onCharEnd() {
        currentCharName = null
        sendAck("char_end", true)
    }

    // ------------------------------------------------------------
    // Outgoing BLE
    // ------------------------------------------------------------

    private val outQueue = ConcurrentLinkedQueue<ByteArray>()
    private var pumpJob: Job? = null

    fun enqueue(bytes: ByteArray) {
        outQueue.add(bytes)
        if (pumpJob == null || pumpJob?.isActive != true) {
            pumpJob = scope.launch { pump() }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun pump() {
        while (true) {
            val next = outQueue.poll() ?: break
            val dev = connectedDevice ?: break
            val tx = txChar ?: break
            val server = gattServer ?: break

            // Fragment at MTU-3
            val frag = (mtu - 3).coerceAtLeast(20)
            var off = 0
            while (off < next.size) {
                val end = minOf(off + frag, next.size)
                val slice = next.copyOfRange(off, end)
                tx.value = slice
                try {
                    server.notifyCharacteristicChanged(dev, tx, false)
                } catch (e: Exception) {
                    Log.w(TAG, "notify failed", e)
                }
                off = end
                // small pacing to let the stack breathe
                kotlinx.coroutines.delay(8)
            }
        }
    }

    private fun sendAck(cmd: String, ok: Boolean, n: Long = 0L, error: String? = null) {
        enqueue(BuddyMessages.ack(cmd, ok, n, error).toByteArray(Charsets.UTF_8))
    }

    /**
     * Called by the UI when the user taps approve/deny on a pending prompt.
     */
    fun sendDecision(id: String, decision: String) {
        enqueue(BuddyMessages.permission(id, decision).toByteArray(Charsets.UTF_8))
        if (decision == "once") {
            // Calculate speed for heart bonus
            val promptedAt = lastPromptAtMs
            val fast = if (promptedAt > 0) System.currentTimeMillis() - promptedAt else Long.MAX_VALUE
            store.recordApproval(fast)
        } else {
            store.recordDeny()
        }
        lastPromptAtMs = 0L
    }

    private var lastPromptAtMs: Long = 0L

    // ------------------------------------------------------------
    // System helpers
    // ------------------------------------------------------------

    private fun hasBlePermissions(): Boolean {
        val s = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) ==
                PackageManager.PERMISSION_GRANTED
        } else true
        return s
    }

    private fun getBatteryPct(): Int? {
        val bm = getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
        val pct = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return if (pct in 0..100) pct else null
    }

    private fun isOnCharger(): Boolean {
        val bm = getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
        return bm.isCharging
    }

    // ------------------------------------------------------------
    // Notification
    // ------------------------------------------------------------

    private fun startForegroundWithNotif() {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_ble),
                NotificationManager.IMPORTANCE_LOW
            )
            nm.createNotificationChannel(ch)
        }
        val notif = buildNotif(connected = false, attention = false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIF_ID, notif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            @Suppress("DEPRECATION")
            startForeground(NOTIF_ID, notif)
        }
    }

    private fun updateNotif(connected: Boolean, attention: Boolean) {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        nm.notify(NOTIF_ID, buildNotif(connected, attention))
    }

    private fun buildNotif(connected: Boolean, attention: Boolean): Notification {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pi = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val text = when {
            attention -> getString(R.string.notif_attention)
            connected -> getString(R.string.notif_connected)
            else      -> getString(R.string.notif_idle)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(pi)
            .setSilent(!attention)
            .build()
    }

    companion object {
        private const val TAG = "BuddyBleService"
        private const val CHANNEL_ID = "buddy_ble"
        private const val NOTIF_ID = 1001

        @JvmStatic @Volatile
        var instance: BuddyBleService? = null
            private set

        fun start(context: Context) {
            val i = Intent(context, BuddyBleService::class.java)
            ContextCompat.startForegroundService(context, i)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, BuddyBleService::class.java))
        }
    }
}
