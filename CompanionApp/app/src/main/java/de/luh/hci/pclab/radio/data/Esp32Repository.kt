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
import java.util.UUID

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

/**
 * Talks to the radio ESP32 over BLE (GATT). Control/data run over BLE while the
 * phone streams audio to the same ESP over A2DP in parallel -- the two Bluetooth
 * links are independent. (This replaces the old Bluetooth-Classic/SPP link that
 * the throwaway coin-counter sketch used.)
 *
 * GATT layout mirrors the firmware in SR/:
 *   AMP service  8A5D0001  -> Source (write), Volume (write)
 *   GAME service 8A5D0010  -> CoinCount (notify), Dispense (write)
 */
@SuppressLint("MissingPermission")
class Esp32Repository(private val context: Context) {

    companion object {
        val AMP_SERVICE: UUID = UUID.fromString("8A5D0001-1111-2222-3333-123456789ABC")
        val SOURCE_CHAR: UUID = UUID.fromString("8A5D0002-1111-2222-3333-123456789ABC")
        val VOLUME_CHAR: UUID = UUID.fromString("8A5D0003-1111-2222-3333-123456789ABC")

        val GAME_SERVICE: UUID = UUID.fromString("8A5D0010-1111-2222-3333-123456789ABC")
        val COINCOUNT_CHAR: UUID = UUID.fromString("8A5D0011-1111-2222-3333-123456789ABC")
        val DISPENSE_CHAR: UUID = UUID.fromString("8A5D0012-1111-2222-3333-123456789ABC")

        // Standard Client Characteristic Configuration descriptor (enables notify).
        val CCCD: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        // Audio source values understood by the firmware's Source characteristic.
        const val SOURCE_DAC = 0    // Bluetooth audio (A2DP) -> used for the music app
        const val SOURCE_RADIO = 1
        const val SOURCE_MUTE = 2
    }

    private val adapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private val scanner get() = adapter?.bluetoothLeScanner

    private var gatt: BluetoothGatt? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState = _connectionState.asStateFlow()

    private val _availableDevices = MutableStateFlow<List<DeviceInfo>>(emptyList())
    val availableDevices = _availableDevices.asStateFlow()

    // Current jackpot, fed by CoinCount notifications from the ESP.
    private val _coinCount = MutableStateFlow(0)
    val coinCount = _coinCount.asStateFlow()

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

    fun startScan() {
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
            enableCoinNotifications(g)
            _connectionState.value = ConnectionState.CONNECTED
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid == COINCOUNT_CHAR) {
                _coinCount.value = characteristic.value?.firstOrNull()?.toInt()?.and(0xFF) ?: 0
            }
        }
    }

    fun connect(device: DeviceInfo) {
        stopScan()
        val btDevice = adapter?.getRemoteDevice(device.address) ?: return
        _connectionState.value = ConnectionState.CONNECTING
        _coinCount.value = 0
        gatt = btDevice.connectGatt(context, false, gattCallback)
    }

    fun disconnect() {
        gatt?.disconnect()
        // onConnectionStateChange will clean up and flip the state to DISCONNECTED.
    }

    private fun cleanup() {
        try { gatt?.close() } catch (_: Exception) {}
        gatt = null
    }

    // ---- Commands ---------------------------------------------------------

    /** Select what the amplifier plays: [SOURCE_DAC] (music/A2DP), [SOURCE_RADIO], [SOURCE_MUTE]. */
    fun setSource(source: Int) = writeByte(AMP_SERVICE, SOURCE_CHAR, source)

    /** Set attenuation 0 (loud) .. 63 (mute) on the LM1971. */
    fun setVolume(attenuation: Int) = writeByte(AMP_SERVICE, VOLUME_CHAR, attenuation)

    /** Pay out [coins] on a casino win. The ESP runs the motor and notifies the new count. */
    fun dispense(coins: Int) = writeByte(GAME_SERVICE, DISPENSE_CHAR, coins)

    @Suppress("DEPRECATION")
    private fun writeByte(service: UUID, characteristic: UUID, value: Int) {
        val g = gatt ?: return
        val ch = g.getService(service)?.getCharacteristic(characteristic) ?: return
        ch.value = byteArrayOf((value and 0xFF).toByte())
        g.writeCharacteristic(ch)
    }

    @Suppress("DEPRECATION")
    private fun enableCoinNotifications(g: BluetoothGatt) {
        val ch = g.getService(GAME_SERVICE)?.getCharacteristic(COINCOUNT_CHAR) ?: return
        g.setCharacteristicNotification(ch, true)
        val cccd = ch.getDescriptor(CCCD) ?: return
        cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        g.writeDescriptor(cccd)
    }
}
