package de.luh.hci.pclab.radio.data

import android.Manifest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import de.luh.hci.pclab.radio.model.DeviceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn


import kotlinx.coroutines.flow.StateFlow

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

interface Esp32Repository {
    val connectionState: StateFlow<ConnectionState>
    val availableDevices: StateFlow<List<DeviceInfo>>
    fun startScan()
    suspend fun connect(device: DeviceInfo): ConnectionState
    suspend fun disconnect()
    fun incomingLines(): Flow<String>
}

// also includes bluetooth functionalities
@SuppressLint("MissingPermission")
class BluetoothEsp32Repository(context: Context) : Esp32Repository {
    private val sppUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val adapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private var socket: BluetoothSocket? = null
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState = _connectionState.asStateFlow()

    private val _availableDevices = MutableStateFlow<List<DeviceInfo>>(emptyList())
    override val availableDevices = _availableDevices.asStateFlow()

    override fun startScan() {
        val bonded = adapter?.bondedDevices.orEmpty()
        _availableDevices.value = bonded.map { DeviceInfo(it.name ?: "Unknown", it.address) }
    }

    override suspend fun connect(device: DeviceInfo): ConnectionState {
        _connectionState.value = ConnectionState.CONNECTING
        val success = withContext(Dispatchers.IO) {
            try {
                val btDevice = adapter?.getRemoteDevice(device.address) ?: return@withContext false
                adapter.cancelDiscovery()
                val s = btDevice.createRfcommSocketToServiceRecord(sppUuid)
                s.connect() // blocks until connected or throws
                socket = s
                true
            } catch (e: Exception) {
                try { socket?.close() } catch (_: Exception) {}
                socket = null
                false
            }
        }
        _connectionState.value =
            if (success) ConnectionState.CONNECTED else ConnectionState.DISCONNECTED
        return _connectionState.value
    }

    override suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            try { socket?.close() } catch (_: Exception) {}
            socket = null
        }
        _connectionState.value = ConnectionState.DISCONNECTED
    }


    override fun incomingLines(): Flow<String> = flow {
        val reader = socket?.inputStream?.bufferedReader() ?: return@flow
        try {
            while (true) {
                val line = reader.readLine() ?: break  // blocks until a \n arrives
                emit(line)
            }
        } catch (_: Exception) { /* socket closed */ }
    }.flowOn(Dispatchers.IO)
}

