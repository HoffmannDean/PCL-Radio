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
 * Provides information on the selected device
 */
class DeviceViewModel(
    private val esp32repo: Esp32Repository
) : ViewModel() {
    private val _connectedDevice = MutableStateFlow<Device?>(null)
    val device: StateFlow<Device?> = _connectedDevice.asStateFlow()

    val availableDevices: StateFlow<List<DeviceInfo>> = esp32repo.availableDevices
    val connectionState = esp32repo.connectionState

    init {
        searchAvailableDevices()
    }

    fun searchAvailableDevices() {
        esp32repo.startScan()
    }

    fun connect(deviceInfo: DeviceInfo) {
        viewModelScope.launch {
            val result = esp32repo.connect(deviceInfo)
            if (result == ConnectionState.CONNECTED) {
                _connectedDevice.value = Device(deviceInfo)
            }
        }
    }

    fun disconnect() {
        // TODO: disconnect from device
        viewModelScope.launch {
            esp32repo.disconnect()
            _connectedDevice.value = null
        }
    }

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