package de.luh.hci.pclab.radio.data

import android.content.Context
import de.luh.hci.pclab.radio.model.DeviceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * A dummy implementation of Esp32Repository for debugging purposes without a physical device.
 */
class Esp32RepositoryDummy(context: Context? = null) : Esp32Repository {
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _availableDevices = MutableStateFlow<List<DeviceInfo>>(emptyList())
    override val availableDevices: StateFlow<List<DeviceInfo>> = _availableDevices.asStateFlow()

    override fun startScan() {
        _availableDevices.value = listOf(
            DeviceInfo("Dummy ESP32 Radio", "00:11:22:33:44:55"),
            DeviceInfo("Simulated Device", "AA:BB:CC:DD:EE:FF")
        )
    }

    override suspend fun connect(device: DeviceInfo): ConnectionState {
        _connectionState.value = ConnectionState.CONNECTING
        delay(1500) // Simulate network/bluetooth latency
        _connectionState.value = ConnectionState.CONNECTED
        return _connectionState.value
    }

    override suspend fun disconnect() {
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    override fun incomingLines(): Flow<String> = flow {
        if (_connectionState.value != ConnectionState.CONNECTED) {
            return@flow
        }
        
        var count = 0
        while (_connectionState.value == ConnectionState.CONNECTED) {
            emit("DUMMY_DATA: Signal strength -${(30..90).random()} dBm (msg: ${count++})")
            delay(2000)
        }
    }.flowOn(Dispatchers.IO)
}
