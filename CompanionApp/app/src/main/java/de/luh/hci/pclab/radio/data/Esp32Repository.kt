package de.luh.hci.pclab.radio.data

import de.luh.hci.pclab.radio.model.DeviceInfo
import kotlinx.coroutines.flow.StateFlow

interface Esp32Repository {
    val connectionState: StateFlow<ConnectionState>
    val availableDevices: StateFlow<List<DeviceInfo>>
    val coinCount: StateFlow<Int>

    /** Battery charge in percent (0..100), pushed from the ESP's Battery Level characteristic. */
    val batteryLevel: StateFlow<Int>

    /** Currently selected audio source: [SOURCE_DAC], [SOURCE_RADIO] or [SOURCE_MUTE]. */
    val source: StateFlow<Int>

    /** Current LM1971 attenuation, [VOLUME_LOUD] (loud) .. [VOLUME_MUTE] (quiet). */
    val volume: StateFlow<Int>

    fun startScan()
    fun connect(device: DeviceInfo)
    fun disconnect()

    /** Select what the amplifier plays: [SOURCE_DAC] (music/A2DP), [SOURCE_RADIO], [SOURCE_MUTE]. */
    fun setSource(source: Int)

    /** Set attenuation [VOLUME_LOUD] (loud) .. [VOLUME_MUTE] (quiet) on the LM1971. */
    fun setVolume(attenuation: Int)

    /** Pay out [coins] on a casino win. The ESP runs the motor and notifies the new count. */
    fun dispense(coins: Int)

    companion object {
        // Audio source values understood by the firmware's Source characteristic.
        const val SOURCE_DAC = 0    // Bluetooth audio (A2DP) -> used for the music app
        const val SOURCE_RADIO = 1
        const val SOURCE_MUTE = 2

        // Attenuation range of the Volume characteristic (0 = loudest, 64 = quietest).
        const val VOLUME_LOUD = 0
        const val VOLUME_MUTE = 64
    }
}
