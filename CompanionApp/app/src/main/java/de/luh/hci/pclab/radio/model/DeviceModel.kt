package de.luh.hci.pclab.radio.model

/*
 * Provides device info that is needed to connect to one specific device
 */
data class DeviceInfo (
    val name: String,
    val address: String
)

data class Device(
    val info: DeviceInfo,
    val coins: Int = 0
)