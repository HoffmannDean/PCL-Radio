package de.luh.hci.pclab.radio.data

import android.Manifest
import de.luh.hci.pclab.radio.model.DeviceInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

// also includes bluetooth functionalities
class Esp32Repository {
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState = _connectionState.asStateFlow()

    private val _availableDevices = MutableStateFlow<List<DeviceInfo>>(emptyList())
    val availableDevices = _availableDevices.asStateFlow()

    fun startScan() {
        _availableDevices.value = emptyList()
        // TODO: retrieve devices
        // scanner.startScan(scanCallback)
        // scanCallback is responsible for updating the available devices
        // TODO: remove below
        _availableDevices.value = listOf(
            DeviceInfo("ESP32-Sensor-01", "00:1A:7D:DA:71:13"),
            DeviceInfo("ESP32-Sensor-02", "00:1A:7D:DA:71:14"),
            DeviceInfo("ESP32-Sensor-03", "00:1A:7D:DA:71:15"),
        )
    }

    suspend fun connect(device: DeviceInfo): ConnectionState {
        _connectionState.value = ConnectionState.CONNECTING
        // TODO: connect to device
        delay(500) // Simulate connection delay
        val success = true // or false if not successful
        if (success) {
            // TODO: disable scanner
            _connectionState.value = ConnectionState.CONNECTED
        } else {
            _connectionState.value = ConnectionState.DISCONNECTED
        }
        return _connectionState.value
    }

    suspend fun disconnect() {
        // TODO: disconnect from device
        delay(500) // Simulate disconnection delay
        _connectionState.value = ConnectionState.DISCONNECTED
    }
}