package de.luh.hci.pclab.radio.data

import de.luh.hci.pclab.radio.model.DeviceInfo
import kotlinx.coroutines.flow.StateFlow

interface Esp32Repository {
    val connectionState: StateFlow<ConnectionState>
    val availableDevices: StateFlow<List<DeviceInfo>>
    val coinCount: StateFlow<Int>

    fun startScan()
    fun connect(device: DeviceInfo)
    fun disconnect()

    /** Select what the amplifier plays: [SOURCE_DAC] (music/A2DP), [SOURCE_RADIO], [SOURCE_MUTE]. */
    fun setSource(source: Int)

    /** Set attenuation 0 (loud) .. 63 (mute) on the LM1971. */
    fun setVolume(attenuation: Int)

    /** Pay out [coins] on a casino win. The ESP runs the motor and notifies the new count. */
    fun dispense(coins: Int)

    companion object {
        // Audio source values understood by the firmware's Source characteristic.
        const val SOURCE_DAC = 0    // Bluetooth audio (A2DP) -> used for the music app
        const val SOURCE_RADIO = 1
        const val SOURCE_MUTE = 2
    }
}
