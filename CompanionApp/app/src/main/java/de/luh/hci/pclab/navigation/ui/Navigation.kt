package de.luh.hci.pclab.navigation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import de.luh.hci.pclab.apps.casino.ui.CasinoView
import de.luh.hci.pclab.apps.selection.ui.SelectionView
import de.luh.hci.pclab.connectivity.ui.ConnectivityStatusBar
import de.luh.hci.pclab.connectivity.ui.ConnectivityView
import de.luh.hci.pclab.radio.data.ConnectionState
import de.luh.hci.pclab.radio.data.DeviceViewModel
import kotlinx.serialization.Serializable

@Serializable
object Connectivity

@Serializable
object AppSelection

@Serializable
object MusicApp

@Serializable
object CasinoApp

@Composable
fun Navigation(
    deviceViewModel: DeviceViewModel = viewModel(factory = DeviceViewModel.Factory)
) {
    val navController = rememberNavController()
    val connectedDevice by deviceViewModel.device.collectAsStateWithLifecycle()
    val connectionState by deviceViewModel.connectionState.collectAsStateWithLifecycle()
    val availableDevices by deviceViewModel.availableDevices.collectAsStateWithLifecycle()

    LaunchedEffect(deviceViewModel) {
        deviceViewModel.searchAvailableDevices()
    }

    LaunchedEffect(connectionState) {
        when (connectionState) {
            ConnectionState.CONNECTED -> navController.navigate(AppSelection) {
                // prevent back navigation to Connectivity
                popUpTo(Connectivity) { inclusive = true }
            }

            ConnectionState.DISCONNECTED -> navController.navigate(Connectivity) {
                popUpTo(0) { inclusive = true }
            }

            ConnectionState.CONNECTING -> Unit
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            ConnectivityStatusBar(
                connectedDevice = connectedDevice,
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Connectivity,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<Connectivity> {
                ConnectivityView(
                    devices = availableDevices,
                    connectionState = connectionState,
                    onDeviceSelected = { deviceInfo ->
                        deviceViewModel.connect(deviceInfo)
                    }
                )
            }

            composable<AppSelection> {
                SelectionView(
                    onAppSelected = { app ->
                        when (app.id) {
                            "casino" -> navController.navigate(CasinoApp)
                            "music" -> navController.navigate(MusicApp)
                        }
                    }
                )
            }

            composable<CasinoApp> {
                CasinoView(onSubmit = {
                    println("Submitted: $it")
                })
            }

            composable<MusicApp> {
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
