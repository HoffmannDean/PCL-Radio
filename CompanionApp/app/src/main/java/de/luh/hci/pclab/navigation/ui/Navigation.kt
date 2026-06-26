package de.luh.hci.pclab.navigation.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import de.luh.hci.pclab.apps.casino.ui.CasinoView
import de.luh.hci.pclab.apps.selection.ui.SelectionView
import de.luh.hci.pclab.connectivity.ui.ConnectivityStatusBar
import de.luh.hci.pclab.connectivity.ui.ConnectivityView
import de.luh.hci.pclab.apps.music.model.Album
import de.luh.hci.pclab.radio.data.ConnectionState
import de.luh.hci.pclab.radio.ui.DeviceViewModel
import kotlinx.serialization.Serializable
import androidx.navigation.toRoute
import de.luh.hci.pclab.apps.music.ui.AlbumDetailView
import de.luh.hci.pclab.apps.music.ui.AlbumsView
import de.luh.hci.pclab.apps.music.ui.PlayerViewModel
import kotlinx.serialization.json.Json

@Serializable
object Connectivity

@Serializable
object AppSelection

@Serializable
object MusicApp


@Serializable
data class AlbumDetail(
    val albumJson: String,
)

@Serializable
object CasinoApp

@Composable
fun Navigation(
    deviceViewModel: DeviceViewModel = viewModel(factory = DeviceViewModel.Factory),
    playerViewModel: PlayerViewModel = viewModel(factory = PlayerViewModel.Factory)
) {
    val navController = rememberNavController()
    val connectedDevice by deviceViewModel.device.collectAsStateWithLifecycle()
    val connectionState by deviceViewModel.connectionState.collectAsStateWithLifecycle()
    val availableDevices by deviceViewModel.availableDevices.collectAsStateWithLifecycle()

    LaunchedEffect(connectionState) {
        when (connectionState) {
            ConnectionState.CONNECTED -> {
                // Only move to Selection if we are currently on the Connectivity screen
                if (navController.currentDestination?.hasRoute<Connectivity>() == true) {
                    navController.navigate(AppSelection) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            }

            ConnectionState.DISCONNECTED -> {
                if (navController.currentDestination?.hasRoute<Connectivity>() == false) {
                    navController.navigate(Connectivity) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
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
                AlbumsView(onAlbumClick = { album ->
                    val albumJson = Json.encodeToString(album)
                    navController.navigate(AlbumDetail(albumJson))
                })
            }

            composable<AlbumDetail> { backStackEntry ->
                val route = backStackEntry.toRoute<AlbumDetail>()
                val album = Json.decodeFromString<Album>(route.albumJson)
                AlbumDetailView(album = album, playerViewModel = playerViewModel)
            }
        }
    }
}
