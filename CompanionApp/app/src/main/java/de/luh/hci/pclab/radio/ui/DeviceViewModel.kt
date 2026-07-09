package de.luh.hci.pclab.radio.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.luh.hci.pclab.RadioApp
import de.luh.hci.pclab.radio.data.ConnectionState
import de.luh.hci.pclab.radio.data.Esp32Repository
import de.luh.hci.pclab.radio.model.Device
import de.luh.hci.pclab.radio.model.DeviceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/*
 * Provides information on the selected device.
 * The coin count and connection state are driven by the ESP32 over BLE.
 */
class DeviceViewModel(
    private val esp32repo: Esp32Repository
) : ViewModel() {
    private val _connectedDevice = MutableStateFlow<Device?>(null)
    val device: StateFlow<Device?> = _connectedDevice.asStateFlow()

    val availableDevices: StateFlow<List<DeviceInfo>> = esp32repo.availableDevices
    val connectionState = esp32repo.connectionState

    // Jackpot, pushed from the ESP via CoinCount notifications.
    val counter: StateFlow<Int> = esp32repo.coinCount

    private var pendingDevice: DeviceInfo? = null

    init {
        // Keep the "connected device" in sync with the BLE connection state.
        viewModelScope.launch {
            esp32repo.connectionState.collect { state ->
                _connectedDevice.value = when (state) {
                    ConnectionState.CONNECTED -> pendingDevice?.let { Device(it) }
                    ConnectionState.DISCONNECTED -> null
                    ConnectionState.CONNECTING -> _connectedDevice.value
                }
            }
        }
    }

    fun searchAvailableDevices() {
        esp32repo.startScan()
    }

    fun connect(deviceInfo: DeviceInfo) {
        pendingDevice = deviceInfo
        esp32repo.connect(deviceInfo)
    }

    fun disconnect() {
        esp32repo.disconnect()
    }

    /** Switch the amplifier to Bluetooth audio (A2DP) for the music app. */
    fun selectMusicSource() = esp32repo.setSource(Esp32Repository.SOURCE_DAC)

    fun setVolume(attenuation: Int) = esp32repo.setVolume(attenuation)

    /** Pay out the whole jackpot on a casino win; the ESP resets the count. */
    fun dispense(coins: Int) = esp32repo.dispense(coins)

    companion object {
        val Factory : ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val esp32repo =
                    (this[ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY] as RadioApp).esp32Repo
                DeviceViewModel(esp32repo)
            }
        }
    }
}
