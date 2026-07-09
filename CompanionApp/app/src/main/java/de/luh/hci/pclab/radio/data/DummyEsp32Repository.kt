package de.luh.hci.pclab.radio.data

import de.luh.hci.pclab.radio.model.DeviceInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DummyEsp32Repository : Esp32Repository {

    private val scope = CoroutineScope(Dispatchers.Main)

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState = _connectionState.asStateFlow()

    private val _availableDevices = MutableStateFlow<List<DeviceInfo>>(emptyList())
    override val availableDevices = _availableDevices.asStateFlow()

    private val _coinCount = MutableStateFlow(10)
    override val coinCount = _coinCount.asStateFlow()

    override fun startScan() {
        _availableDevices.value = listOf(
            DeviceInfo("Dummy ESP32", "00:11:22:33:44:55"),
            DeviceInfo("Another Device", "AA:BB:CC:DD:EE:FF")
        )
    }

    override fun connect(device: DeviceInfo) {
        scope.launch {
            _connectionState.value = ConnectionState.CONNECTING
            delay(1000)
            _connectionState.value = ConnectionState.CONNECTED
        }
    }

    override fun disconnect() {
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    override fun setSource(source: Int) {
        // No-op in dummy
    }

    override fun setVolume(attenuation: Int) {
        // No-op in dummy
    }

    override fun dispense(coins: Int) {
        scope.launch {
            val currentCount = _coinCount.value
            val newCount = (currentCount - coins).coerceAtLeast(0)
            // Simulate count-down notifications
            for (i in currentCount downTo newCount) {
                _coinCount.value = i
                delay(200)
            }
        }
    }
}
