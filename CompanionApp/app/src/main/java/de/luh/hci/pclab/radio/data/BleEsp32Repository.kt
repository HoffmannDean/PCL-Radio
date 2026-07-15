package de.luh.hci.pclab.radio.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import de.luh.hci.pclab.radio.model.DeviceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.ArrayDeque
import java.util.UUID

@SuppressLint("MissingPermission")
class BleEsp32Repository(private val context: Context) : Esp32Repository {

    companion object {
        val AMP_SERVICE: UUID = UUID.fromString("8A5D0001-1111-2222-3333-123456789ABC")
        val SOURCE_CHAR: UUID = UUID.fromString("8A5D0002-1111-2222-3333-123456789ABC")
        val VOLUME_CHAR: UUID = UUID.fromString("8A5D0003-1111-2222-3333-123456789ABC")

        val GAME_SERVICE: UUID = UUID.fromString("8A5D0010-1111-2222-3333-123456789ABC")
        val COINCOUNT_CHAR: UUID = UUID.fromString("8A5D0011-1111-2222-3333-123456789ABC")
        val DISPENSE_CHAR: UUID = UUID.fromString("8A5D0012-1111-2222-3333-123456789ABC")

        // Standard Battery Service (0x180F) with the Battery Level characteristic (0x2A19).
        val BATTERY_SERVICE: UUID = uuid16("180F")
        val BATTERY_LEVEL_CHAR: UUID = uuid16("2A19")

        // Standard Client Characteristic Configuration descriptor (enables notify).
        val CCCD: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        /** Expand a 16-bit BLE UUID (e.g. "180F") to its 128-bit base form. */
        private fun uuid16(short: String): UUID =
            UUID.fromString("0000$short-0000-1000-8000-00805f9b34fb")
    }

    private val adapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private val scanner get() = adapter?.bluetoothLeScanner

    private var gatt: BluetoothGatt? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState = _connectionState.asStateFlow()

    private val _availableDevices = MutableStateFlow<List<DeviceInfo>>(emptyList())
    override val availableDevices = _availableDevices.asStateFlow()

    // Current jackpot, fed by CoinCount notifications from the ESP.
    private val _coinCount = MutableStateFlow(0)
    override val coinCount = _coinCount.asStateFlow()

    // Battery charge in percent, fed by Battery Level notifications from the ESP.
    private val _batteryLevel = MutableStateFlow(0)
    override val batteryLevel = _batteryLevel.asStateFlow()

    // Current audio source / volume. Read once on connect, then kept in sync with our own writes.
    private val _source = MutableStateFlow(Esp32Repository.SOURCE_DAC)
    override val source = _source.asStateFlow()

    private val _volume = MutableStateFlow(Esp32Repository.VOLUME_MUTE)
    override val volume = _volume.asStateFlow()

    // ---- Scanning ---------------------------------------------------------

    private val found = LinkedHashMap<String, DeviceInfo>()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = device.name ?: result.scanRecord?.deviceName ?: "Unknown"
            found[device.address] = DeviceInfo(name, device.address)
            _availableDevices.value = found.values.toList()
        }
    }

    override fun startScan() {
        found.clear()
        _availableDevices.value = emptyList()
        // No service-UUID filter: the ESP advertises two 128-bit UUIDs, which
        // don't fit in the 31-byte advertisement, so filtering by UUID would
        // miss it. List every nearby BLE device and let the user pick "MyMusic".
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        scanner?.startScan(null, settings, scanCallback)
    }

    private fun stopScan() {
        try { scanner?.stopScan(scanCallback) } catch (_: Exception) {}
    }

    // ---- GATT operation queue --------------------------------------------
    // Android allows only one outstanding GATT operation (read/write/descriptor
    // write) at a time; the next may only start once the previous one's callback
    // fires. Serialize everything through this queue so the connect-time reads
    // and notification setups don't clobber each other.

    private val gattOps = ArrayDeque<() -> Unit>()
    private var gattBusy = false

    @Synchronized
    private fun enqueueOp(op: () -> Unit) {
        gattOps.add(op)
        if (!gattBusy) runNextOp()
    }

    @Synchronized
    private fun runNextOp() {
        if (gattBusy) return
        val op = gattOps.poll() ?: return
        gattBusy = true
        op()
    }

    /** Signal that the current GATT operation finished, so the next one may start. */
    @Synchronized
    private fun completeOp() {
        gattBusy = false
        runNextOp()
    }

    @Synchronized
    private fun clearOps() {
        gattOps.clear()
        gattBusy = false
    }

    // ---- Connection -------------------------------------------------------

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                g.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                cleanup()
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                g.disconnect()
                return
            }
            // Subscribe to the notifying characteristics and read the current
            // amplifier state so the UI starts in sync with the device.
            enableNotifications(GAME_SERVICE, COINCOUNT_CHAR)
            enableNotifications(BATTERY_SERVICE, BATTERY_LEVEL_CHAR)
            readCharacteristic(BATTERY_SERVICE, BATTERY_LEVEL_CHAR)
            readCharacteristic(AMP_SERVICE, SOURCE_CHAR)
            readCharacteristic(AMP_SERVICE, VOLUME_CHAR)
            _connectionState.value = ConnectionState.CONNECTED
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            // Notifications are unsolicited and are NOT part of the op queue.
            when (characteristic.uuid) {
                COINCOUNT_CHAR -> _coinCount.value = characteristic.byteValue()
                BATTERY_LEVEL_CHAR -> _batteryLevel.value = characteristic.byteValue()
            }
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicRead(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                when (characteristic.uuid) {
                    BATTERY_LEVEL_CHAR -> _batteryLevel.value = characteristic.byteValue()
                    SOURCE_CHAR -> _source.value = characteristic.byteValue()
                    VOLUME_CHAR -> _volume.value = characteristic.byteValue()
                }
            }
            completeOp()
        }

        override fun onCharacteristicWrite(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            completeOp()
        }

        override fun onDescriptorWrite(
            g: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            completeOp()
        }
    }

    override fun connect(device: DeviceInfo) {
        stopScan()
        val btDevice = adapter?.getRemoteDevice(device.address) ?: return
        _connectionState.value = ConnectionState.CONNECTING
        _coinCount.value = 0
        clearOps()
        gatt = btDevice.connectGatt(context, false, gattCallback)
    }

    override fun disconnect() {
        gatt?.disconnect()
        // onConnectionStateChange will clean up and flip the state to DISCONNECTED.
    }

    private fun cleanup() {
        clearOps()
        try { gatt?.close() } catch (_: Exception) {}
        gatt = null
    }

    // ---- Commands ---------------------------------------------------------

    override fun setSource(source: Int) {
        _source.value = source
        writeByte(AMP_SERVICE, SOURCE_CHAR, source)
    }

    override fun setVolume(attenuation: Int) {
        val clamped = attenuation.coerceIn(Esp32Repository.VOLUME_LOUD, Esp32Repository.VOLUME_MUTE)
        _volume.value = clamped
        writeByte(AMP_SERVICE, VOLUME_CHAR, clamped)
    }

    override fun dispense(coins: Int) = writeByte(GAME_SERVICE, DISPENSE_CHAR, coins)

    @Suppress("DEPRECATION")
    private fun writeByte(service: UUID, characteristic: UUID, value: Int) {
        val g = gatt ?: return
        enqueueOp {
            val ch = g.getService(service)?.getCharacteristic(characteristic)
            if (ch == null) { completeOp(); return@enqueueOp }
            ch.value = byteArrayOf((value and 0xFF).toByte())
            g.writeCharacteristic(ch)
        }
    }

    @Suppress("DEPRECATION")
    private fun readCharacteristic(service: UUID, characteristic: UUID) {
        val g = gatt ?: return
        enqueueOp {
            val ch = g.getService(service)?.getCharacteristic(characteristic)
            if (ch == null) { completeOp(); return@enqueueOp }
            g.readCharacteristic(ch)
        }
    }

    @Suppress("DEPRECATION")
    private fun enableNotifications(service: UUID, characteristic: UUID) {
        val g = gatt ?: return
        enqueueOp {
            val ch = g.getService(service)?.getCharacteristic(characteristic)
            if (ch == null) { completeOp(); return@enqueueOp }
            g.setCharacteristicNotification(ch, true)
            val cccd = ch.getDescriptor(CCCD)
            if (cccd == null) { completeOp(); return@enqueueOp }
            cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            g.writeDescriptor(cccd)
        }
    }
}

/** Read the first byte of a characteristic as an unsigned 0..255 value. */
@Suppress("DEPRECATION")
private fun BluetoothGattCharacteristic.byteValue(): Int =
    value?.firstOrNull()?.toInt()?.and(0xFF) ?: 0
