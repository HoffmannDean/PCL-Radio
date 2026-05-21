package de.luh.hci.pclab.navigation.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import de.luh.hci.pclab.apps.casino.ui.CasinoView
import de.luh.hci.pclab.apps.selection.ui.SelectionView
import de.luh.hci.pclab.connectivity.ui.ConnectivityStatusBar
import de.luh.hci.pclab.connectivity.ui.ConnectivityView
import de.luh.hci.pclab.connectivity.ui.Device

enum class Screen {
    Selection,
    Casino,
    Music
}

@Composable
fun Navigation() {
    var connectedDevice by remember { mutableStateOf<Device?>(null) }
    var currentScreen by remember { mutableStateOf(Screen.Selection) }

    val dummyDevices = listOf(
        Device("ESP32-Sensor-01", "00:1A:7D:DA:71:13"),
        Device("ESP32-Sensor-02", "00:1A:7D:DA:71:14"),
        Device("ESP32-Sensor-03", "00:1A:7D:DA:71:15"),
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            ConnectivityStatusBar(connectedDevice = connectedDevice)
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            if (connectedDevice == null) {
                ConnectivityView(
                    devices = dummyDevices,
                    onDeviceSelected = {
                        connectedDevice = it
                        currentScreen = Screen.Selection
                    }
                )
            } else {
                when (currentScreen) {
                    Screen.Selection -> {
                        SelectionView(
                            onAppSelected = { app ->
                                when (app.id) {
                                    "casino" -> currentScreen = Screen.Casino
                                    "music" -> currentScreen = Screen.Music
                                }
                            }
                        )
                    }
                    Screen.Casino -> {
                        BackHandler {
                            currentScreen = Screen.Selection
                        }
                        CasinoView()
                    }
                    Screen.Music -> {
                        BackHandler {
                            currentScreen = Screen.Selection
                        }
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Music App (Not implemented)")
                        }
                    }
                }
            }
        }
    }
}
